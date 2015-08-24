package com.wolterskluwer.service.content.validation.validator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.wolterskluwer.service.content.validation.Params;
import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.context.OrchestrationContext;
import com.wolterskluwer.service.content.validation.reporter.Reporter;

/**
 * Date: 6/11/13
 */
public class IPACKStructureValidator implements Validator {

    private OrchestrationContext context;
    private Reporter reporter;
    private List<String> inputPaths;

    /**
     * Parameter from orchestration.xml to filter the input filename. If it
     * matches we have a SUCCESS, if it doesn't match we have an error:
     */
    private static final String PARAM_REGEX = "filename.regex";
    private String fileRegex = "";

    protected void init(OrchestrationContext context, Params params, Reporter reporter,
            String... paths) {
        this.context = context;
        this.reporter = reporter;
        this.inputPaths = Collections.unmodifiableList(Arrays.asList(paths));
        String regex = params.getParam(PARAM_REGEX);
        if (regex != null && !"".equals(regex)) {
            fileRegex = regex;
        }
    }

    @Override
    public void validate(OrchestrationContext context, Params params, Reporter reporter,
            String... paths) throws ValidationException {
        init(context, params, reporter, paths);
        try {
            ensureFileNameIsCorrect(true);
            String rootPath = ensureRootDirectoryExists();
            String manifestFilePath = ensureManifestFileExists(rootPath);
            List<String> expectedDirPaths = composeExpectedDirList(rootPath, manifestFilePath);
            ensureSubDirPaths(expectedDirPaths, manifestFilePath);
        } catch (ValidationInterruptedException ex) {
            // validation is interrupted
        }
    }

    private boolean ensureFileNameIsCorrect(boolean stopProcessing)
            throws ValidationInterruptedException {
        if (fileRegex.isEmpty()) {
            return true;
        } else {
            String name = context.getInputFileNameWithExtention();
            boolean matched = Pattern.matches(fileRegex, name);
            if (!matched && stopProcessing) {
                fatal(String.format("Filename %s doesn't match regular expression %s", name,
                        fileRegex));
            }
            return matched;
        }
    }

    private boolean containsPath(String path) {
        return inputPaths.contains(path);
    }

    private String ensureRootDirectoryExists() throws ValidationInterruptedException {
        String name = context.getInputFileName();
        String rootPath = Paths.directory(name);
        ensureOneRootDirPath(rootPath);
        if (!containsPath(rootPath)) {
            fatal(String.format("Root level does not contain folder %s (case-sensitive match)",
                    name));
        }
        return rootPath;
    }

    private void ensureOneRootDirPath(String rootPath) {
        List<String> rootPaths = pathsByLevel(0);
        rootPaths.remove(rootPath);
        for (String path : rootPaths) {
            error(String.format("Root level contains invalid folder %s (case-sensitive match)",
                    path));
        }
    }

    private String ensureManifestFileExists(String rootPath) throws ValidationInterruptedException {
        String manifestFilePath = Paths.concat(rootPath, "interchange-package-properties.xml");
        if (!containsPath(manifestFilePath)) {
            fatal("File interchange-package-properties.xml could not be found");
        }
        return manifestFilePath;
    }

    private void ensureSubDirPaths(List<String> expectedDirPaths, String manifestFilePath) {
        ensureExpectedDirPathsExist(expectedDirPaths);
        ensureSubDirPathsAreExpected(expectedDirPaths, manifestFilePath);
    }

    private void ensureExpectedDirPathsExist(List<String> expectedDirPaths) {
        for (String expectedDirPath : expectedDirPaths) {
            int count = entryCount(expectedDirPath);
            if (count == 0) {
                error(String.format("Folder %s does not exist in IPACK (case-sensitive match)",
                        expectedDirPath));
            } else if (count == 1) {
                error(String.format("Folder %s is empty", expectedDirPath));
            }
        }
    }

    private void ensureSubDirPathsAreExpected(List<String> expectedDirPaths, String manifestFilePath) {
        List<String> subDirPaths = pathsByLevel(1);
        subDirPaths.remove(manifestFilePath);
        for (String subDirPath : subDirPaths) {
            if (!expectedDirPaths.contains(subDirPath)) {
                error(String
                        .format("Folder %s not mentioned in interchange-package-properties.xml (case-sensitive match)",
                                subDirPath));
            }
        }
    }

    private int entryCount(String expectedDirPath) {
        int count = 0;
        for (String inputPath : inputPaths) {
            if (inputPath.startsWith(expectedDirPath)) {
                count++;
            }
        }
        return count;
    }

    private List<String> composeExpectedDirList(String rootPath, String manifestFilePath)
            throws ValidationException {
        List<String> subDirNames = extractDirNamesFromManifest(manifestFilePath);
        return toPaths(rootPath, subDirNames);
    }

    private List<String> extractDirNamesFromManifest(String manifestFilePath)
            throws ValidationInterruptedException {
        ArrayList<String> nodeNames = new ArrayList<String>();
        try {
            Document doc = parseXML(context.getInputStream(manifestFilePath));
            NodeList nodes = doc.getElementsByTagName("interchange-subdirectories");
            if (nodes.getLength() == 0) {
                return nodeNames;
            }
            NodeList childNodes = nodes.item(0).getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    nodeNames.add(node.getNodeName());
                }
            }
        } catch (Exception e) {
            fatal("Could not parse interchange-package-properties.xml");
        }
        return nodeNames;
    }

    private List<String> toPaths(String baseDirPath, List<String> subDirNames) {
        ArrayList<String> paths = new ArrayList<String>();
        for (String name : subDirNames) {
            paths.add(Paths.concat(baseDirPath, Paths.directory(name)));
        }
        return paths;
    }

    private Document parseXML(InputStream inputStream) throws IOException, SAXException {
        XMLParser parser = new XMLParser();
        parser.setValidating(false);
        return parser.parseXML(inputStream);
    }

    /**
     * Retrieves a list of paths with the given <code>level</code>.
     * 
     * @param level
     *            the level to filter paths
     * @return a list ot paths with the given <code>level</code>
     */
    private List<String> pathsByLevel(int level) {
        ArrayList<String> matchedPaths = new ArrayList<String>();
        for (String path : inputPaths) {
            if (Paths.level(path) == level) {
                matchedPaths.add(path);
            }
        }
        return matchedPaths;
    }

    /**
     * Logs an error message in the {@link #reporter}.
     * 
     * @param message
     *            a message to log
     */
    private void error(String message) {
        reporter.error(message);
    }

    /**
     * Logs an error message in the {@link #reporter} and throws it as an
     * exception in order to interrupt subsequent validations.
     * 
     * @param message
     *            an error message to log.
     * @throws ValidationInterruptedException
     */
    private void fatal(String message) throws ValidationInterruptedException {
        reporter.error(message);
        throw new ValidationInterruptedException(message);
    }

    /**
     * Utility class, provides a set of methods for manipulating string
     * representations of paths.
     * 
     */
    static class Paths {

        /**
         * Contains a character which is used as a separator for path tokens.
         * It's not recommended to change the value as all paths passed in the
         * validator follow the fixed convention: they are relative, they use
         * "/" as a separator, and directory paths end with "/".
         */
        private static final String SEPARATOR = "/";

        /**
         * Determines whether the given path points to a directory.
         * 
         * @param path
         *            a path to test
         * @return true if the given path points to a directory, false otherwise
         */
        public static boolean isDirectory(String path) {
            return path.endsWith(SEPARATOR);
        }

        /**
         * Creates a directory paths from the given path by appending (if
         * needed) the {@link #SEPARATOR} at the end.
         * 
         * @param path
         *            original path
         * @return a directory path based on the original path
         */
        public static String directory(String path) {
            if (!isDirectory(path)) {
                return path + SEPARATOR;
            }
            return path;
        }

        /**
         * Determines the level of the given path. For root paths the level is
         * 0, for its sub-folders the level is 1 etc.
         * 
         * @param path
         *            a path to determine its level
         * @return the level of the given path
         */
        public static int level(String path) {
            int level = StringUtils.countMatches(path, SEPARATOR);
            if (isDirectory(path)) {
                level--;
            }
            return level;
        }

        /**
         * Concatenates two tokens in one path assuming that the
         * <code>basePath</code> points to a directory, if not it will be casted
         * to a directory path, and then concatenated with the <code>path</code>
         * .
         * 
         * @param basePath
         *            the base path pointing to a directory
         * @param path
         *            a path to append to the base
         * @return the newly created path string
         */
        public static String concat(String basePath, String path) {
            if (!isDirectory(basePath)) {
                basePath = directory(basePath);
            }
            return basePath + path;
        }
    }

    @SuppressWarnings("serial")
    static class ValidationInterruptedException extends ValidationException {
        public ValidationInterruptedException(String message) {
            super(message);
        }
    }
}
