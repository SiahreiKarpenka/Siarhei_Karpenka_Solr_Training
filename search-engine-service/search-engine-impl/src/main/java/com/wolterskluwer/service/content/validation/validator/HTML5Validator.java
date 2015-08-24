package com.wolterskluwer.service.content.validation.validator;


import com.wolterskluwer.service.content.validation.Params;
import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.context.OrchestrationContext;
import com.wolterskluwer.service.content.validation.reporter.Reporter;
import nu.validator.validation.SimpleDocumentValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class HTML5Validator implements Validator {

	private static final String DEFAULT_SCHEMA_URL_VALUE = "http://s.validator.nu/html5-all.rnc";
	private static final boolean DEFAULT_LOAD_ENTITIES_VALUE = true;
	private static final boolean DEFAULT_NO_STREAM_VALUE = false;
	public static final String PARAM_SCHEMA_URL = "schema.url";
	public static final String PARAM_NO_STREAM = "no.stream";
	public static final String PARAM_LOAD_ENTITIES = "load.entities";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HTML5Validator.class);

	private SimpleDocumentValidator validator;
	private Reporter reporter;
	private String schemaUrl;
	private boolean noStream;
	private boolean loadEntities;
	private OrchestrationContext context;

	@Override
	public void validate(OrchestrationContext context, Params params,
			Reporter reporter, String... paths) throws ValidationException {
		try {
			init(context, params, reporter);
            for (String path : paths) {
                validateFileByPath(path);
            }
		} catch (Throwable e) {
			LOGGER.error("Error on HTML5 validation.", e);
			reporter.error(e.getMessage());
		}
	}

	private void validateFileByPath(String path) {
		try {
            Reporter contentReporter = reporter.getResourceReporter(path);
            
            initiateValidator(contentReporter);

            InputSource inputSource = new InputSource(context.getInputStream(path));
			validator.checkHtmlInputSource(inputSource);
        } catch (Exception e) {
            // catch all exceptions during the validation and report them in order not to stop
            // subsequent validations
            reporter.error(e.getMessage());
        }
	}

	private void initiateValidator(Reporter contentReporter)
			throws Exception {
		HTML5ErrorHandler handler = new HTML5ErrorHandler(contentReporter);
		validator.setUpMainSchema(schemaUrl, handler);
		validator.setUpValidatorAndParsers(handler, noStream, loadEntities);
	}

	private void init(OrchestrationContext context, Params params,
			Reporter reporter) throws Exception {
		validator = new SimpleDocumentValidator();
		schemaUrl = params.getParam(PARAM_SCHEMA_URL);
		schemaUrl = schemaUrl != null ? schemaUrl : DEFAULT_SCHEMA_URL_VALUE;
		String noStreamStr = params.getParam(PARAM_NO_STREAM);
		noStream = noStreamStr != null ? Boolean.parseBoolean(noStreamStr)
				: DEFAULT_NO_STREAM_VALUE;
		String loadEntitiesStr = params.getParam(PARAM_LOAD_ENTITIES);
		loadEntities = loadEntitiesStr != null ? Boolean
				.parseBoolean(loadEntitiesStr) : DEFAULT_LOAD_ENTITIES_VALUE;
		this.context = context;
		this.reporter = reporter;
	}

	private class HTML5ErrorHandler implements ErrorHandler {

		private Reporter reporter;

		public HTML5ErrorHandler(Reporter reporter) {
			this.reporter = reporter;
		}

		@Override
		public void error(SAXParseException error) throws SAXException {
			reporter.error(getLogMessage(error));
		}

		@Override
		public void fatalError(SAXParseException error) throws SAXException {
			reporter.error(getLogMessage(error));
		}

		@Override
		public void warning(SAXParseException warning) throws SAXException {
			reporter.warn(getLogMessage(warning));
		}

		private String getLogMessage(SAXParseException e) {
			StringBuilder builder = new StringBuilder();
			builder.append("Line:");
			builder.append(e.getLineNumber());
			builder.append(", column:");
			builder.append(e.getColumnNumber());
			builder.append(" - ");
			builder.append(e.getMessage());
			return builder.toString();
		}
	}
}