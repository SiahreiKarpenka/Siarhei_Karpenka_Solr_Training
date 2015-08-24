package com.wolterskluwer.service.content.validation.validator;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.xml.resolver.CatalogManager;

import com.wolterskluwer.csg.pci.api.PciApiException;
import com.wolterskluwer.csg.pci.api.v3.PciPackage;
import com.wolterskluwer.pci.schema.v3.Action;
import com.wolterskluwer.pci.schema.v3.Manifest;
import com.wolterskluwer.service.content.validation.ConfigurationResourceAccessException;
import com.wolterskluwer.service.content.validation.Messages;
import com.wolterskluwer.service.content.validation.Params;
import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.context.OrchestrationContext;
import com.wolterskluwer.service.content.validation.reporter.Reporter;

/**
 * PackageStructureValidator validates Folders in the package against
 * manifest.xml
 */
public class PackageStructureValidator implements Validator {

    private static final Messages messages = Messages.getInstance();

    /**
     * Normal path separator character. This character is always used as a path separator regardless
     * of the system path separator.
     *
     * @see #normalizeFilePath(String)
     */
    public static final char PATH_SEPARATOR_CHAR = '/';

    /**
     * The name of the PCI pack manifest file. This name is used to find the manifest in the list of
     * input file paths.
     *
     * @see #inputFilePaths
     */
    public static final String MANIFEST_FILENAME = "manifest.xml";

    /**
     * The name of a parameter that specifies the path to a catalog file which will be used to
     * resolve external resources during XML parsing.
     */
    public static final String PARAM_PATH_CATALOG = "path.catalog";

    /**
     * The name of a parameter that specifies whether the folder paths (the paths that don't end
     * with a file name) are allowed.
     *
     * Originally, this parameter was used only with Interchange, that's where the parameter
     * comes from. This parameter is be renamed to something like "allow.folders" in the future.
     *
     */
    public static final String PARAM_ALLOW_INTERCHANGE = "allow.interchange";

    /**
     * Contains input file paths. All paths in this collection are normalized. Intended to
     * store file paths relative to a PCI pack's root.
     *
     * @see #normalizeFilePath(String)
     */
    private List<String> inputFilePaths = new ArrayList<String>();

    /**
     * Contains relative file paths read from the manifest.xml including manifest.xml. This
     * collection is actually contains all the expected file paths for the given pack. All paths
     * in this collection are normalized.
     *
     * @see #normalizeFilePath(String)
     */
    private List<String> manifestFilePaths = new ArrayList<String>();

    /**
     * Specifies whether to allow interchange file resources in the input pack. If this field set to
     * <code>true</code>, all interchange files found in the pack won't be treated as unexpected
     * assuming that the manifest contains <code>add-replace</code> element specifying interchange
     * folder aspect validation. Basically this field contains the value of the
     * {@link #PARAM_ALLOW_INTERCHANGE} parameter, and by default is set to <code>false</code>.
     */
    private boolean allowInterchange = false;

    /**
     * List of folder paths extracted from the manifest.xml file. These paths come from
     * add-replace elements with aspect="folder".
     * <p>
     * This list gets populated only if the #allowInterchange property is <code>true</code>
     * @see #allowInterchange
     * @see #manifestFolderPaths
     */
    private List<String> manifestFolderPaths = new ArrayList<String>();

    /**
     * Identifies whether the manifest.xml file path is presented in the input file paths list.
     * @see #MANIFEST_FILENAME
     */
    private boolean manifestExists = false;

    /**
     * a <code>CatalogManager</code> instance that will be used to resolve external resource
     * references during parsing of manifest.xml. This instance is configured only once per
     * the validate() method call.
     *
     * @see #configureCatalogManager(String)
     * @see #PARAM_PATH_CATALOG
     */
    private CatalogManager catalogManager = null;

    /** The orchestration context passed to the validator */
    private OrchestrationContext context;

    /** <code>Reporter</code> instance passed to the validator */
    private Reporter reporter;

    /**
     * Initializes the main properties of the validator.
     *
     * @param context   an <code>OrchestrationContext</code> instance passed in the validator
     * @param params    a <code>Params</code> data passed in the validator
     * @param reporter  a reporter passed in the validator
     *
     * @see #validate(OrchestrationContext, Params, Reporter, String...)
     */
    private void init(OrchestrationContext context, Params params, Reporter reporter)
            throws ConfigurationResourceAccessException {
        this.reporter = reporter;
        this.context = context;
        applyParams(params);
    }

    /**
     * Initializes properties with respect to the parameters passed to the validator.
     * <p>
     * Currently, this method initializes only the allowInterchange and catalogManager
     * properties
     * @param params a <code>Params</code> instance to apply
     * @see #allowInterchange
     * @see #catalogManager
     */
    private void applyParams(Params params) throws ConfigurationResourceAccessException {
        if (params == null) {
            // can be null in unit tests!
            return;
        }
        allowInterchange = Boolean.parseBoolean(params.getParam(PARAM_ALLOW_INTERCHANGE));
        catalogManager = configureCatalogManager(params.getParam(PARAM_PATH_CATALOG));
    }

    private CatalogManager configureCatalogManager(String catalogFilePath)
            throws ConfigurationResourceAccessException {
        CatalogManager catalogManager = new CatalogManager();
        catalogManager.setRelativeCatalogs(false);
        catalogManager.setPreferPublic(true);
        catalogManager.setUseStaticCatalog(false);
        catalogManager.setAllowOasisXMLCatalogPI(true);
        if (isNotEmpty(catalogFilePath)) {
            catalogManager.setCatalogFiles(resolvePath(catalogFilePath));
        }
        return catalogManager;
    }

    /**
     * Method to validate extracted package against folder structure Assumption
     * is that manifest.xml file was validated against schema
     *
     * @param context  an {@link OrchestrationContext} instance which will be used for retrieving
     *                 configuration and input file resources
     * @param params   validation name-value parameter pairs
     * @param paths    an array of input file paths to validate the pack structure
     * @param reporter an instance of {@link Reporter} which will apply all error messages reported
     *                 by this validator
     */
    @Override
    public void validate(OrchestrationContext context, Params params, Reporter reporter,
                         String... paths) throws ValidationException {
        try {
            init(context, params, reporter);
            validatePaths(paths);
        } catch (Exception e) {
            reporter.error(e.getMessage());
        }
    }

    private void validatePaths(String[] paths) throws ValidationException, PciApiException {
        scanInputPaths(paths);
        if (!manifestExists) {
            throw new ValidationException(messages.getMessage("msg.missingManifest"));
        }
        scanManifestPaths(MANIFEST_FILENAME);
        validateManifestFilePaths();
        validateInputFilePaths();
    }

    private void scanInputPaths(String[] paths) {
        for (String path : paths) {
            if (!isDirectory(path)) {
                if (MANIFEST_FILENAME.equals(path)) {
                    manifestExists = true;
                } else {
                    inputFilePaths.add(normalizeFilePath(path));
                }
            }
        }
    }

    private void scanManifestPaths(String manifestPath) throws ValidationException, PciApiException {
        PciPackage pciPackage = getPciPackage(getInputStream(manifestPath));
        if (allowInterchange) {
            scanManifestPathsAllowFolder(pciPackage);
        } else {
            scanManifestPaths(pciPackage);
        }
    }

    /**
     * Extracts all the paths defined in the package's manifest.xml.
     * @param pciPackage a <code>PciPackage</code> instance to scan for the manifest files
     * @see #manifestFilePaths
     */
    private void scanManifestPaths(PciPackage pciPackage) {
        Collection<Action> actions = pciPackage.getAddReplaceActions();
        for (Action action : actions) {
            manifestFilePaths.add(action.getAddReplace().getObjectMCPPathAttr());
        }
    }

    /**
     * Extracts all the paths defined in the package's manifest.xml; additionally it recognizes
     * the folder paths (the paths defined in an add-replace element with aspect="folder".
     * @param pciPackage
     * @see #inputFilePaths
     * @see #manifestFolderPaths
     * @see #hasFolderAspect(com.wolterskluwer.pci.schema.v3.Action)
     */
    private void scanManifestPathsAllowFolder(PciPackage pciPackage) {
        Collection<Action> actions = pciPackage.getAddReplaceActions();
        for (Action action : actions) {
            String path = action.getAddReplace().getObjectMCPPathAttr();
            if (hasFolderAspect(action)) {
                manifestFolderPaths.add(path);
            } else {
                manifestFilePaths.add(path);
            }
        }
    }

    /**
     * Determines whether the given Action instance has an add-replace element with aspect="folder".
     * @param action an <code>Action</code> instant to test
     * @return
     */
    private static boolean hasFolderAspect(Action action) {
        return "folder".equals(action.getAddReplace().getAspect().value());
    }

    private PciPackage getPciPackage(InputStream manifestStream)
            throws PciApiException, ValidationException {
        PciPackage pciPackage = new PciPackage();
        try {
            Manifest manifest = PciPackage.readManifest(manifestStream, catalogManager);
            // in order to be abe to call PciPackage instance methods it's important to set the
            // manifest property because the the readManifest() method doesn't set this property,
            // it just returns a reference to the newly created Manifest instance
            pciPackage.setManifest(manifest);
        } catch (PciApiException ex) {
            throw new ValidationException(getErrorMessage("msg.xml.parseError", ex), ex);
        } finally {
            IOUtils.closeQuietly(manifestStream);
        }
        return pciPackage;
    }

    /**
     * Turns a relative path into its absolute representation
     * @param path
     * @return an absolute path
     * @throws ConfigurationResourceAccessException
     */
    private String resolvePath(String path) throws ConfigurationResourceAccessException {
        return context.getConfiguration().getFileResource(path).getAbsolutePath();
    }

    private InputStream getInputStream(String path) throws ValidationException {
        return context.getInputStream(path);
    }

    private boolean isPathAllowed(String path) {
        for (String directoryPath : manifestFolderPaths) {
            if (path.startsWith(directoryPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method search for the files mentioned in manifest inside PCI Package
     */
    private void validateManifestFilePaths() throws ValidationException {
        for (String fileName : manifestFilePaths) {
            if (!inputFilePaths.contains(fileName)) {
                reporter.error(messages.getMessage("msg.missedResource") + fileName);
            }
        }
    }

    /**
     * This method search for the files which exist in PCI Package but are not
     * mentioned in manifest
     */
    private void validateInputFilePaths() throws ValidationException {
        for (String packageFileName : inputFilePaths) {
            if (!manifestFilePaths.contains(packageFileName)) {
                if (!isPathAllowed(packageFileName)) {
                    reporter.error(messages.getMessage("msg.unexpectedResource") + packageFileName);
                }
            }
        }
    }

    private static String getErrorMessage(String key, Exception ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(MANIFEST_FILENAME);
        sb.append(':');
        sb.append(' ');
        sb.append(messages.getMessage(key));
        sb.append(ex.getMessage());
        return sb.toString();
    }

    /**
     * Replaces all occurrences of <code>\</code> (backslash) with the normal
     * {@link #PATH_SEPARATOR_CHAR}.
     *
     * @param filePath a file path to normalize
     * @return normalized path
     */
    private static String normalizeFilePath(String filePath) {
        if (filePath.indexOf('\\') != -1) {
            filePath = filePath.replace('\\', PATH_SEPARATOR_CHAR);
        }
        return filePath;
    }

    private static boolean isDirectory(String path) {
        char lastChar = path.charAt(path.length() - 1);
        return lastChar == PATH_SEPARATOR_CHAR || lastChar == '\\';
    }
}
