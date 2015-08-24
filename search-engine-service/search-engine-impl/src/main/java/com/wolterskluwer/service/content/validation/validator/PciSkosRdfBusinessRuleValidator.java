package com.wolterskluwer.service.content.validation.validator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.xml.resolver.CatalogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Model;
import com.wolterskluwer.csg.pci.api.PciApiException;
import com.wolterskluwer.csg.pci.api.v3.PciPackage;
import com.wolterskluwer.csg.pci.api.v3.filters.AspectFilter;
import com.wolterskluwer.pci.schema.v3.Action;
import com.wolterskluwer.pci.schema.v3.AspectType;
import com.wolterskluwer.service.content.validation.ConfigurationResourceAccessException;
import com.wolterskluwer.service.content.validation.Messages;
import com.wolterskluwer.service.content.validation.Params;
import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.reporter.Reporter;

public class PciSkosRdfBusinessRuleValidator
        extends AbstractBusinessRuleValidator
        implements Validator {

    private static final Logger logger =
            LoggerFactory.getLogger(PciSkosRdfBusinessRuleValidator.class);

    /**
     * Contains the manifest file name. This file is expected to be in the root
     * directory of a PCI package, so that the path to it is always equal to its
     * name.
     */
    private static final String MANIFEST_FILEPATH = "manifest.xml";

    /**
     * The name of the parameter that specifies a relative path to the catalog,
     * which will be used to resolve external entities while parsing XML files.
     * The catalog is also important in order to parse the manifest file
     * correctly as it may contain references to external files such as
     * <code>entities.dtd</code>
     * 
     */
    private static final String PARAM_CATALOG_PATH = "path.catalog";

    PciSkosRdfBusinessRuleValidator(ValidationContext context) {
        /*
         * The super class requires the constructor which applies an
         * <code>ValidationContext</code> instance
         */
        super(context);
    }

    @Override
    protected void validateContent(String[] paths) {
        try {
            validate(Arrays.asList(paths));
        } catch (Exception e) {
            // catch and log all the exception thrown during the validation
            // process
            getReporter().error(e.getMessage());
        }
    }

    private void validate(List<String> paths) throws ValidationException, PciApiException,
            IOException, SAXException, ParserConfigurationException {
        if (!paths.contains(MANIFEST_FILEPATH)) {
            fatal("manifest.xml is not found");
        }
        Reporter reporter = getReporter();

        List<String> inputFiles = scanManifestForSkosFilePaths(getOrchestrationContext()
                .getInputStream(MANIFEST_FILEPATH));
        if (!inputFiles.isEmpty()) {
            List<String> sparqlQueries = getQueriesFromProperties();
            for (String path : inputFiles) {
                try {
                    setReporter(reporter.getResourceReporter(path));
                    validateModel(readModel(path), sparqlQueries);
                } catch (ValidationException e) {
                    logger.error("Error during reading model: ", e);
                    getReporter().error(e.getMessage());
                }
            }
        }
    }

    /**
     * Retrieves an RDF model from the input pack with by the given relative
     * path.
     * 
     * @param path
     *            a path to get an RDF model
     * @return a model with the given relative path
     * @throws ValidationException
     */
    private Model readModel(String path) throws ValidationException {
        return getRdfModel(path);
        // return getRdfModel(getOrchestrationContext().getInputStream(path),
        // path);
    }

    List<String> scanManifestForSkosFilePaths(InputStream manifestStream)
            throws ValidationException, PciApiException {
        try {
            Collection<Action> actions = getKosSchemeActions(manifestStream);
            ArrayList<String> skosFiles = new ArrayList<String>();
            for (Action action : actions) {
                skosFiles.add(action.getAddReplace().getObjectMCPPathAttr());
            }
            return skosFiles;
        } finally {
            IOUtils.closeQuietly(manifestStream);
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<Action> getKosSchemeActions(InputStream in) throws PciApiException,
            ConfigurationResourceAccessException {
        String catalogPath = getCatalogFilePath();
        PciPackage pciPackage = new PciPackage();
        if (catalogPath != null) {
            pciPackage.loadManifest(in, configureCatalogManager(catalogPath));
        } else {
            pciPackage.loadManifest(in);
        }
        return pciPackage.filterActions(new AspectFilter(AspectType.KOS_SCHEME));
    }

    private String getCatalogFilePath() {
        Params params = getParams();
        if (params != null) {
            String path = params.getParam(PARAM_CATALOG_PATH);
            if (path == null || "".equals(path)) {
                return null;
            }
            return path;
        }
        return null;
    }

    private CatalogManager configureCatalogManager(String catalogFilePath)
            throws ConfigurationResourceAccessException {
        CatalogManager catalogManager = new CatalogManager();
        catalogManager.setRelativeCatalogs(false);
        catalogManager.setVerbosity(0);
        catalogManager.setPreferPublic(false);
        catalogManager.setUseStaticCatalog(false);
        catalogManager.setAllowOasisXMLCatalogPI(true);
        catalogManager.setCatalogFiles(resolvePath(catalogFilePath));
        return catalogManager;
    }

    private String resolvePath(String path) throws ConfigurationResourceAccessException {
        return getConfiguration().getFileResource(path).getAbsolutePath();
    }

    private void fatal(String message) throws ValidationException {
        getReporter().error(message);
        throw new ValidationException(message);
    }

    @Override
    void addValidationError(String jenaError) {
        // TODO Auto-generated method stub
        getReporter().error(Messages.getInstance().getMessage("msg.pci.skos.invalid") + jenaError);
    }
}
