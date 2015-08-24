package com.wolterskluwer.service.content.validation;

import com.wolterskluwer.service.content.validation.context.OrchestrationContext;
import com.wolterskluwer.service.content.validation.context.OrchestrationContextFactory;
import com.wolterskluwer.service.content.validation.orchestration.*;
import com.wolterskluwer.service.content.validation.orchestration.Context.Filters.Path;
import com.wolterskluwer.service.content.validation.reporter.CompositeReporter;
import com.wolterskluwer.service.content.validation.reporter.ReporterFactory;
import com.wolterskluwer.service.content.validation.reporter.SimpleReporter;
import com.wolterskluwer.service.content.validation.reporter.UpdatableReporter;
import com.wolterskluwer.service.content.validation.util.Message;
import com.wolterskluwer.service.content.validation.validator.Validator;
import com.wolterskluwer.service.content.validation.validator.ValidatorFactory;
import com.wolterskluwer.service.discovery.api.PlanArtifact;
import com.wolterskluwer.service.mime.MimeType;
import com.wolterskluwer.service.mime.MimeTypeUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrchestrationExecutor {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(OrchestrationExecutor.class);

	private static final String ORCHESTRATION_XML = "orchestration.xml";

	protected StopWatch clock = new StopWatch();

	ValidationServiceConfiguration configuration;

	private CompositeReporter reporter = createCompositeReporter();

	private ValidatorFactory validatorFactory = new ValidatorFactory();

	private InternalValidationReport validationReport = new InternalValidationReport();

	private Map<String, String> queryParameters = new HashMap<String, String>();

	private MimeType mimeType;

	private OrchestrationContext orchestrationContext;

	private boolean isPackage;

	private static SimpleReporter internalReporter;

	OrchestrationExecutor(
			ValidationServiceConfiguration configuration,
			Map<String, String> properties,
			String contentObjectId,
			String data, MimeType mimeType) throws ValidationException {

		try {
			if (properties != null) {
				this.queryParameters.putAll(properties);
			}
			this.configuration = configuration;
			ensureMimeTypeIsExpected(configuration.getPlan(), mimeType);

			this.mimeType = mimeType;
			this.orchestrationContext =
					OrchestrationContextFactory.createContext(
							configuration, contentObjectId, data, mimeType);
			this.orchestrationContext.validate();
			this.orchestrationContext.initReporter(reporter);
			this.isPackage = orchestrationContext.isPackage();
		} catch (Exception e) {
			LOGGER.error("Exception occurred during validation creation", e);
			getReporter().error(e.toString());
			throw new ValidationException(
					"Validation execution initialization failed: " + e.toString());
		}
	}

	public void ensureMimeTypeIsExpected(PlanArtifact plan, MimeType mimeType)
			throws MimeTypeIsNotSupportedException {
		List<String> rawExpectedMimeTypes = plan.getManifest().getInput();
		List<MimeType> expectedMimeTypes = MimeTypeUtil.parseAll(rawExpectedMimeTypes);

		if (!MimeTypeUtil.matchesToAtLeastOneOf(mimeType, expectedMimeTypes)) {
			throw new MimeTypeIsNotSupportedException(String.format(
					"MIME type '%s' is not supported by %s [%s].",
					mimeType, plan.getName(), plan.getVersion()));
		}
	}

	/**
	 * Parses the orchestration.xml file into internal object tree and retrieves
	 * its root object.
	 *
	 * @return the root of the object tree created from orchestration.xml
	 * @throws ValidationException
	 */
	private Orchestration parseOrchestration() throws ValidationException {
		File orchestrationFile = configuration.getFileResource(ORCHESTRATION_XML);
		OrchestrationUnmarshaller unmarshaller = new OrchestrationUnmarshaller();
		return unmarshaller.unmarshallResolveParams(orchestrationFile, queryParameters);
	}

	private void ensureOrchestrationExists() throws FileNotFoundException {
		try {
			configuration.getFileResource(ORCHESTRATION_XML);
		} catch (Exception e) {
			String message = Messages.getInstance().getMessage("msg.orchestration.notFound");
			getReporter().error(message);
			throw new FileNotFoundException(e.getMessage());
		}
	}

	public ValidationReport performValidation() {
		try {
			LOGGER.info("TIMER " + this.getClass().getName() + "  unmarshalling orchestration");
			clock.start();
			ensureOrchestrationExists();
			Orchestration orchestration = parseOrchestration();
			clock.stop();
			LOGGER.info("TIMER {} unmarshalling orchestration: {}",
					this.getClass().getName(), clock.getTime());
			clock.reset();
			fillOrchestrationReporters(reporter, orchestration);
			reporter.start("Validation was started.");

			List<Context> contexts = orchestration.getContextsByMimeType(mimeType);
			if ((!orchestration.isHandleArchives() || !isPackage)
					&& contexts.size() < 1) {
				throw new ValidationException(
						"There are no validators configured for mime type " + mimeType);
			}
			performValidation(contexts, false);
			if (orchestration.isHandleArchives() && isPackage) {
				contexts = orchestration.getPathContexts();
				performValidation(contexts, true);
			}

			reporter.complete("Validation was completed.");

			onCompleted();
		} catch (Exception e) {
			LOGGER.error("Error during validation: ", e);
			getReporter().error("Error during validation: " + e.toString());
		} finally {
			validationReport.getMessages().addAll(internalReporter.getMessages());
			cleanUp();
		}
		return validationReport;
	}

	private void performValidation(List<Context> contexts, boolean handleArchive)
			throws ValidationException {
		for (Context context : contexts) {
			List<Validation> validations = context.getValidation();

			for (Validation validation : validations) {
				String validatorUri = validation.getRefId();
				Validator validator = validatorFactory.newValidator(validatorUri);
				LOGGER.info("TIMER {} starting validation: ",
						this.getClass().getName(), validator.getClass().getName());
				clock.start();
				try {
					Params params = toParams(validation.getParam());
					if (handleArchive) {
						executeValidator(context.getFilters().getPath(), validator, params, validatorUri);
					} else {
						executeValidator(validator, params, validatorUri);
					}
					if (validation.isBreakOnError()
							&& getReporter().getErrors().size() > 0) {
						break;
					}
				} finally {
					clock.stop();
					LOGGER.info(
							"TIMER " + this.getClass().getName()
									+ " done validation: " + validator.getClass().getName()
									+ " IN: " + clock.getTime());
					clock.reset();
				}
			}
		}
	}

	private static CompositeReporter createCompositeReporter() {
		CompositeReporter compositeReporter = new CompositeReporter();
		internalReporter = new SimpleReporter();
		compositeReporter.addReporter(internalReporter);
		return compositeReporter;
	}

	private void fillOrchestrationReporters(
			CompositeReporter compositeReporter, Orchestration orchestration) {
		if (orchestration.getReporter() == null) {
			return;
		}
		for (XmlReporter reporterConfig : orchestration.getReporter()) {
			UpdatableReporter reporter = ReporterFactory.newReporter(reporterConfig.getId());
			List<Param> parameters = reporterConfig.getParam();
			for (Param param : parameters) {
				reporter.setParameter(param.getName(), param.getValue());
			}
			compositeReporter.addReporter(reporter);
		}
	}

	protected void onCompleted() {
		configuration.getPlan().release();
		reporter.destroy();
	}

	public UpdatableReporter getReporter() {
		return reporter;
	}

	private static Params toParams(List<Param> xmlParams) {
		Params params = new Params();
		for (Param xmlParam : xmlParams) {
			params.addParam(xmlParam.getName(), xmlParam.getValue());
		}
		return params;
	}

	public void cleanUp() {
		try {
			orchestrationContext.cleanUp();
		} catch (Exception e) {
			LOGGER.warn("Warning during validation: ", e);
		}
	}

	protected void executeValidator(Validator validator, Params params,
	                                String validatorUri) throws ValidationException {
		executeValidator(null, validator, params, validatorUri);
	}

	protected void executeValidator(List<Path> paths, Validator validator,
	                                Params params, String validatorUri) throws ValidationException {
		try {
			List<String> filesList = getFilesList(paths);
			if (isMatchedFilesFound(filesList)) {
				String[] files = filesList.toArray(new String[]{});
				validator.validate(orchestrationContext, params, reporter, files);
			}
		} catch (IOException e) {
			throw new ValidationException(e.getMessage(), e);
		}
	}

	private List<String> getFilesList(List<Path> paths) throws IOException {
		if (paths != null) {
			List<String> result = new ArrayList<String>();
			for (Path path : paths) {
				result.addAll(orchestrationContext.getFiles(path.getValue(), path.getCaseSensitive()));
			}
			return result;
		} else {
			return orchestrationContext.getAllFiles();
		}
	}

	private boolean isMatchedFilesFound(List<String> filesList) {
		return !CollectionUtils.isEmpty(filesList);
	}

	private static class InternalValidationReport implements ValidationReport {

		private List<Message> messages = new ArrayList<Message>();

		@Override
		public List<Message> getMessages() {
			return messages;
		}

	}

	public MimeType getMimeType() {
		return mimeType;
	}
}
