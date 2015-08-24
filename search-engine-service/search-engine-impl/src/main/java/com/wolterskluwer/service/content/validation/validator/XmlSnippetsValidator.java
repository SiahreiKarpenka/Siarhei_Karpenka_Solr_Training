package com.wolterskluwer.service.content.validation.validator;

import com.wolterskluwer.service.content.validation.Messages;
import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.ValidationServiceConfiguration;
import com.wolterskluwer.service.content.validation.reporter.Reporter;
import com.wolterskluwer.service.content.validation.util.FailOverXMLCatalogResolver;
import org.apache.xerces.parsers.SAXParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Collections;
import java.util.List;

public class XmlSnippetsValidator extends AbstractRDFValidator implements Validator {

	private static final Logger LOG = LoggerFactory.getLogger(SkosValidator.class);
	static final String PARAM_ONTOLOGY_PATH = "path.ontology"; // ontology files
	static final String PARAM_PATH_CATALOG = "path.catalog";
	// Number of rdf files that will be validated in one XML. (Literals from
	// those RDF files will be extracted into one XML file and validated)
	static final Integer MAX_MODELS_COUNT = 100;

	XmlSnippetsValidator(ValidationContext context) {
		super(context);
	}

	/**
	 * Only for tests usage
	 * 
	 * @param configuration
	 * @param context
	 */
	XmlSnippetsValidator(ValidationServiceConfiguration configuration, ValidationContext context) {
		super(context);
		setConfiguration(configuration);
	}

    @Override
    protected void validateContent(String[] paths) throws ValidationException {
        String[] ontologyPaths = getParams().getParams(PARAM_ONTOLOGY_PATH);
        if (ontologyPaths == null) {
            throwExceptionAndLogError("msg.rdf.ontology.noParam", null);
        }

        SnippetProcessor snippetProcessor = new SnippetProcessor(getModelsFromPaths(ontologyPaths));

        String catalogPath = getParams().getParam(PARAM_PATH_CATALOG);
        if (paths.length > 0) {
            int modelNum = 0;
            int totalCnt = 0;
            for (String path : paths) {
                snippetProcessor.addModel(path, getRdfModel(path));
                modelNum++;
                totalCnt++;
                if (modelNum >= MAX_MODELS_COUNT) {
                    InputSource inputSource = new InputSource(
                            new java.io.StringReader(snippetProcessor.getSnippetXml()));
                    validateInputStream(inputSource, getConfiguration().getFileResource(catalogPath).getAbsolutePath(),
                            getReporter());
                    modelNum = 0;
                    snippetProcessor.cleanModelMap();
                    LOG.info("Processing XML snippet validation for: " + totalCnt);
                }
            }
        } else {
            snippetProcessor.addModel("local.file", getRdfModel("local.file"));
        }

        InputSource inputSource = new InputSource(new java.io.StringReader(snippetProcessor.getSnippetXml()));
        LOG.info(snippetProcessor.getSnippetXml());

        validateInputStream(inputSource, getConfiguration().getFileResource(catalogPath).getAbsolutePath(), getReporter());
    }



	/**
	 * Mathod that validates InputSource (XML file) using catalog.
	 * 
	 * @param inputSource
	 *            - XML file to validate
	 * @param catalogPath
	 *            - path to catalog
	 * @param reporter
	 *            - reporter to collect errors
	 * @throws ValidationException
	 */
	void validateInputStream(InputSource inputSource, String catalogPath, Reporter reporter) throws ValidationException {
		List<String> catalogPaths = Collections.singletonList(catalogPath);
		com.wolterskluwer.service.content.validation.util.FailOverXMLCatalogResolver resolver = FailOverXMLCatalogResolver
				.initializeAndGetXmlCatalogResolver(catalogPaths);
		// XMLCatalogResolver resolver = new XMLCatalogResolver(new String[]
		// {catalogPath});
		// LSResourceResolver resourceResolver = new
		// CatalogResolver(catalogPath);
		SAXParser parser = new SAXParser();
		try {
			parser.setFeature("http://apache.org/xml/features/validation/schema", true);
			parser.setFeature("http://xml.org/sax/features/validation", true);
			parser.setProperty("http://apache.org/xml/properties/internal/entity-resolver", resolver);
			parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
					"http://www.w3.org/2001/XMLSchema");
			XmlParserErrorHandler handler = new XmlParserErrorHandler(reporter);
			parser.setErrorHandler(handler);
			parser.setContentHandler(handler);
			parser.parse(inputSource);
		} catch (Exception e) {
			String error = Messages.getInstance().getMessage("msg.xml.parserCreation");
			reporter.error(error);
			throw new ValidationException(error, e);
		}

	}

	public class XmlParserErrorHandler extends DefaultHandler implements ErrorHandler {

		private static final String SNIPPET_PROPERTY = "snippet:property";
		private String fileName = "";
		private final Reporter reporter;

		public XmlParserErrorHandler(Reporter reporter) {
			this.reporter = reporter;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (qName.equalsIgnoreCase(SNIPPET_PROPERTY)) {
				String fileAttribute = attributes.getValue("file");
				String[] files = fileAttribute.split(" ");
				for (String file : files) {
					if (file.endsWith(".rdf")) {
						fileName = file;
						break;
					}
				}
			}
		}

		@Override
		public void warning(SAXParseException exception) throws SAXException {
			reporter.warn(fileName + ": " + exception.getMessage());
		}

		@Override
		public void error(SAXParseException exception) throws SAXException {
			reporter.error(fileName + ": " + exception.getMessage());
		}

		@Override
		public void fatalError(SAXParseException exception) throws SAXException {
			reporter.error(fileName + ": " + exception.getMessage());
		}

	}

}
