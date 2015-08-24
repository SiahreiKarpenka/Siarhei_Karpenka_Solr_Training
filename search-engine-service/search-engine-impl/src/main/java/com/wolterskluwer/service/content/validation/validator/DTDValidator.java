package com.wolterskluwer.service.content.validation.validator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.wolterskluwer.service.content.validation.ConfigurationResourceAccessException;
import com.wolterskluwer.service.content.validation.Messages;
import com.wolterskluwer.service.content.validation.Params;
import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.context.OrchestrationContext;
import com.wolterskluwer.service.content.validation.reporter.Reporter;

/**
 * http://wolterskluwer.com/services/validation/DTDValidation
 */
public class DTDValidator implements Validator {

    private static final Messages messages = Messages.getInstance();

    public static final String PARAM_PATH_CATALOG = "path.catalog";
    public static final String PARAM_PATH_DTD = "path.dtd";

    /**
     * The name of the parameter that specifies whether to apply "strict" parsing for the
     * input XML file.
     */
    public static final String PARAM_PARSE_STRICT = "parse.strict";

    private Reporter reporter;

    private OrchestrationContext context;

    private SafeParams params;

    private CatalogResolver catalogResolver;

    /**
     * Path to a DTD file from the configuration pack. This path is absolute and resolved relative
     * to the configuration pack's root.
     */
    private String externalDtd;

    /**
     * XMLFactory instance which will be used to create DocumentBuilders for validation
     */
    private XMLFactory xmlFactory = new XMLFactory();

    /**
     * Initialize the state of the validator. This method should be invoked first within the
     * {@link #validate(OrchestrationContext, Params, Reporter, String...)} method.
     *
     * @param context   an {@link OrchestrationContext} instance passed in the validator
     * @param params    orchestration parameters passed in the validator
     * @param reporter  the reporter to use for error reporting
     */
    private void init(OrchestrationContext context, Params params, Reporter reporter)
            throws ValidationException {
        this.reporter = reporter;
        this.context = context;
        this.params = new SafeParams(params);
        configureParams(this.params);
    }

    private void configureParams(SafeParams params) throws ValidationException {
        String pathDtd = params.getParam(PARAM_PATH_DTD);
        String pathCatalog = params.getParam(PARAM_PATH_CATALOG);

        if (SafeParams.isDefined(pathDtd) && SafeParams.isDefined(pathCatalog)) {
            // both PARAM_PATH_CATALOG and PARAM_PATH_DTD are mutually exclusive and cannot be set
            // both at the same time
            throw new ValidationException(messages.getMessage("msg.dtd.illegalParameterCombination"));
        }

        if (SafeParams.isDefined(pathDtd)) {
            externalDtd = resolvePath(pathDtd);
        }
        if (SafeParams.isDefined(pathCatalog)) {
            catalogResolver = initCatalogResolver(pathCatalog);
        }
    }

    /**
     * Constructs a catalog resolver based on the file located by the given path.
     *
     * @param pathCatalog should not be <code>null</code>; path to the catalog file.
     * @return
     * @throws ConfigurationResourceAccessException
     */
    private CatalogResolver initCatalogResolver(String pathCatalog)
            throws ConfigurationResourceAccessException {
        if (pathCatalog == null) {
            throw new NullPointerException("Parameter 'pathCatalog' cannot be null");
        }
        return new CatalogResolver(resolvePath(pathCatalog));
    }

    private String resolvePath(String path) throws ConfigurationResourceAccessException {
        File configurationFile = context.getConfiguration().getFileResource(path);
        return configurationFile.getAbsolutePath();
    }

    @Override
    public void validate(OrchestrationContext context, Params params, Reporter reporter,
                         String... paths) throws ValidationException {
        try {
            init(context, params, reporter);
            for (String path : paths) {
                validateFileByPath(path);
            }
        } catch (Exception e) {
            reporter.error(e.getMessage());
        }
    }

    /**
     * Validates a single file resource with the given path relative to the input package root.
     *
     * @param path a file path to validate
     * @throws ValidationException
     */
    private void validateFileByPath(String path) throws ValidationException {
        try {
            Reporter contentReporter = reporter.getResourceReporter(path);
            validateInputStream(getInputStream(path, contentReporter), contentReporter);
        } catch (Exception e) {
            // catch all exceptions during the validation and report them in order not to stop
            // subsequent validations
            reporter.error(e.getMessage());
        }
    }

    private InputStream getInputStream(String path, Reporter reporter) throws ValidationException {
        InputStream in;
        if (shouldUseExternalDtd()) {
            in = replaceDoctypeSystemId(context.getInputStream(path), externalDtd, reporter);
        } else {
            in = context.getInputStream(path);
        }
        return in;
    }

    private void validateInputStream(InputStream in, Reporter reporter) throws ValidationException {
        try {
            DocumentBuilder builder = getValidatingBuilder();
            builder.setEntityResolver(catalogResolver); // can be null (the default resolver will be used)
            builder.setErrorHandler(new ErrorReporter(reporter));
            builder.parse(in);
        } catch (Exception e) {
            // IOException | SAXException during parsing
            throw new ValidationException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private DocumentBuilder getValidatingBuilder() throws ParserConfigurationException {
        if (useStrict()) {
            return xmlFactory.getValidatingBuilder();
        } else {
            return xmlFactory.getDynamicBuilder();
        }
    }

    private boolean useStrict() {
        return params.getBoolean(PARAM_PARSE_STRICT);
    }

    private boolean shouldUseExternalDtd() {
        return externalDtd != null;
    }

    private InputStream replaceDoctypeSystemId(InputStream in, String systemId, Reporter reporter)
            throws  ValidationException {
        try {
            Document doc = parseDocument(in, systemId, reporter);
            String publicId = getPublicId(doc.getDoctype());
            return transform(doc, publicId, systemId);
        } catch (Exception e) {
            reporter.error("DOCTYPE has been replaced with " + systemId);
            throw new ValidationException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves publicId from the given <code>DocumentType</code> element.
     *
     * @param doctype a DocumentType instance to get the <code>publicId</code>
     * @return the public ID string or <code>null</code> if the given doctype contains no public ID
     */
    private static String getPublicId(DocumentType doctype) {
        if (doctype == null) {
            // the passed doctype can be null
            return null;
        }
        return doctype.getPublicId();
    }

    private InputStream transform(Document doc, String publicId, String systemId)
            throws TransformerException, IOException {
        Transformer transformer = xmlFactory.getTransformer();

        if (publicId != null) {
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, publicId);
        }
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, systemId);
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        StringWriter writer = new StringWriter();

        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return IOUtils.toInputStream(writer.toString(), "UTF-8");
    }

    private Document parseDocument(InputStream in, String systemId, Reporter reporter)
            throws ParserConfigurationException, IOException, SAXException {
        try {
            DocumentBuilder builder = xmlFactory.getParsingBuilder();
            builder.setEntityResolver(new SingleFileResolver(systemId));
            builder.setErrorHandler(new ErrorReporter(reporter));
            return builder.parse(new InputSource(in));
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private static class SingleFileResolver implements EntityResolver {

        private final String systemId;

        SingleFileResolver(String systemId) {
            this.systemId = systemId;
        }

        private String getSystemId() {
            return systemId;
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException,
                IOException {
            return new InputSource(getSystemId());
        }
    }



    private static class XMLFactory {

        private static final String FEATURE_LOAD_EXTERNAL_DTD =
                "http://apache.org/xml/features/nonvalidating/load-external-dtd";

        public static final String FEATURE_VALIDATION_DYNAMIC =
                "http://apache.org/xml/features/validation/dynamic";

        private static DocumentBuilderFactory factory = createDocumentBuilderFactory();

        private static DocumentBuilderFactory createDocumentBuilderFactory() {
            return DocumentBuilderFactory.newInstance();
        }

        private static void resetFactorySettings() throws ParserConfigurationException {
            factory. setValidating(false);
            factory.setFeature(FEATURE_VALIDATION_DYNAMIC, false);
            factory.setFeature(FEATURE_LOAD_EXTERNAL_DTD, true);
            factory.setNamespaceAware(true);
        }

        private DocumentBuilder validatingBuilder;
        private DocumentBuilder dynamicBuilder;
        private DocumentBuilder parsingBuilder;
        private Transformer transformer;

        public DocumentBuilder getValidatingBuilder() throws ParserConfigurationException {
            if (validatingBuilder == null) {
                resetFactorySettings();
                factory.setValidating(true);
                validatingBuilder = factory.newDocumentBuilder();
            } else {
                validatingBuilder.reset();
            }
            return validatingBuilder;
        }

        public DocumentBuilder getDynamicBuilder() throws ParserConfigurationException {
            if (dynamicBuilder == null) {
                resetFactorySettings();
                factory.setFeature(FEATURE_VALIDATION_DYNAMIC, true);
                factory.setValidating(true);
                dynamicBuilder = factory.newDocumentBuilder();
            } else {
                dynamicBuilder.reset();
            }
            return dynamicBuilder;
        }

        private DocumentBuilder getParsingBuilder() throws ParserConfigurationException {
            if (parsingBuilder == null) {
                resetFactorySettings();
                parsingBuilder = factory.newDocumentBuilder();
            } else {
                parsingBuilder.reset();
            }
            return parsingBuilder;
        }

        public Transformer getTransformer() throws TransformerConfigurationException {
            if (transformer == null) {
                transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
                transformer.setOutputProperty(OutputKeys.INDENT, "no");
            } else {
                transformer.reset();
            }
            return transformer;
        }
    }

    /**
     * Decorator for the {@link Params}. Adds checking for empty param values.
     *
     */
    private static class SafeParams {

        private final Params params;

        /**
         * Constructs <code>SafeParams</code> instance.
         * @param params
         * @throws NullPointerException if <code>params</code> parameter is null
         */
        public SafeParams(Params params) {
            if (params == null) {
                throw new NullPointerException("Parameter 'params' cannot be null");
            }
            this.params = params;
        }

        private static String trimSafely(String value) {
            if (value == null) {
                return value;
            }
            return value.trim();
        }

        /**
         * Retrieves the value of the parameter with the given name in a safe manner. This
         * guarantees that the returned value won't ever be an empty string or a string containing
         * only white-space characters.
         *
         * @param name the name of parameter to get the value
         * @return the trimmed value of the parameter with the given name
         */
        public String getParam(String name) {
            String value = trimSafely(params.getParam(name));
            if ("".equals(value)) {
                return null;
            }
            return value;
        }

        public boolean getBoolean(String name) {
            String value = getParam(name);
            return Boolean.parseBoolean(value);
        }

        public static boolean isDefined(String value) {
            return value != null;
        }
    }

    private static class ErrorReporter implements ErrorHandler {

        private Reporter reporter;

        ErrorReporter(Reporter reporter) {
            this.reporter = reporter;
        }

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            reporter.warn(exception.getMessage());
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            reporter.error(exception.getMessage());
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            reporter.error(exception.getMessage());
        }
    }
}
