package com.wolterskluwer.service.content.validation.validator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.wolterskluwer.service.content.validation.Messages;
import com.wolterskluwer.service.content.validation.Params;
import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.ValidationServiceConfiguration;
import com.wolterskluwer.service.content.validation.context.OrchestrationContext;
import com.wolterskluwer.service.content.validation.reporter.Reporter;

public class XmlValidator implements Validator {

    // required parameters
    static final String PARAM_PATH_SCHEMA = "path.xsd";
    static final String PARAM_PATH_CATALOG = "path.catalog";

    private SchemaFactory schemaFactory = null;
    private ValidationServiceConfiguration configuration;
    private Params params;
    private LSResourceResolver resourceResolver;

    // package-private constructor to prevent instantiating from outside of the package
    XmlValidator() {
        schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    }

    static ErrorHandler createErrorHandler(Reporter reporter) {
        return new ErrorReporter(reporter);
    }

    private static boolean isDefined(String path) {
        return path != null && !"".equals(path);
    }

    @Override
    public void validate(OrchestrationContext context, Params params, Reporter reporter, String... paths)
            throws ValidationException {
        try {
            init(context.getConfiguration(), params);
            File schemaFile = getSchemaFile(params);
            if (paths.length > 0) {
                for (String path : paths) {
                    ErrorHandler errorHandler = createErrorHandler(reporter.getResourceReporter(path));
                    validateInputStream(context.getInputStream(path), schemaFile, errorHandler, path);
                }
			} else if (!context.isPackage()) {
                // TODO this check must be outside of a validator!
                ErrorHandler errorHandler = createErrorHandler(reporter);
                validateInputStream(context.getInputStream(), schemaFile, errorHandler, null);
            }
        } catch (Exception e) {
            reporter.error(e.getMessage());
        }
    }

    void validateInputStream(InputStream inputStream, File schemaFile, ErrorHandler errorHandler, String path)
            throws ParserConfigurationException, IOException, SAXException {
        try {
            Schema schema = createSchemaFromFile(schemaFile);
            javax.xml.validation.Validator validator = schema.newValidator();
            validator.setErrorHandler(errorHandler);
            validator.setResourceResolver(resourceResolver);
            validator.validate(new StreamSource(inputStream, path));
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    void init(ValidationServiceConfiguration configuration, Params params)
            throws IOException, ValidationException {
        this.configuration = configuration;
        this.params = params;
        initResolvers();
    }

    void initResolvers() throws IOException, ValidationException {
        String value = params.getParam(PARAM_PATH_CATALOG);
        if (isDefined(value)) {
            File file = configuration.getFileResource(value);
            initCatalogResolver(file);
        }
    }

    void initCatalogResolver(File catalog) {
        String catalogPath = catalog.getAbsolutePath();
        resourceResolver = new CatalogResolver(catalogPath);
    }

    File getSchemaFile(Params params) throws ValidationException {
        String schemaPath = params.getParam(PARAM_PATH_SCHEMA);
        if (isDefined(schemaPath)) {
            File schemaFile = configuration.getFileResource(schemaPath);
            return schemaFile;
        }
        return null;
    }

    Schema createSchemaFromFile(File schemaFile)
            throws SAXException {
        if (schemaFile == null) {
            return schemaFactory.newSchema();
        } else {
            return schemaFactory.newSchema(schemaFile);
        }
    }

    private static class ErrorReporter implements ErrorHandler {

        private final Reporter reporter;

        public ErrorReporter(Reporter reporter) {
            this.reporter = reporter;
        }

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            reporter.warn(exception.getMessage());
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            reporter.error(Messages.getInstance().getMessage("msg.xml.invalid.xsd")
                    + exception.getMessage());
        }

        @Override
        public void fatalError(SAXParseException exception)
                throws SAXException {
            //reporter.error(exception.getMessage());
        }
    }
}
