package com.wolterskluwer.services.content.validation;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericXmlApplicationContext;

import com.wolterskluwer.osa.content.odata.api.ContentJob;
import com.wolterskluwer.osa.content.odata.api.ContentObject;
import com.wolterskluwer.osa.content.odata.api.ContentService;
import com.wolterskluwer.osa.content.odata.api.GetValidationResult;
import com.wolterskluwer.osa.content.odata.api.GetValidationResultResponse;
import com.wolterskluwer.osa.content.odata.api.GetValidationStatus;
import com.wolterskluwer.osa.content.odata.api.Message;
import com.wolterskluwer.osa.content.odata.api.ObjectData;
import com.wolterskluwer.osa.content.odata.api.PlanIdentifier;
import com.wolterskluwer.osa.content.odata.api.ProcessResult;
import com.wolterskluwer.osa.content.odata.api.ProcessStatus;
import com.wolterskluwer.osa.content.odata.api.Report;
import com.wolterskluwer.osa.content.odata.api.Status;
import com.wolterskluwer.osa.content.odata.api.Validate;
import com.wolterskluwer.osa.content.odata.api.ValidateAsynch;
import com.wolterskluwer.osa.content.odata.api.ValidateResponse;
import com.wolterskluwer.osa.content.odata.client.ContentODataClient;
import com.wolterskluwer.osa.content.odata.client.operationset.ValidationOperationSet;
import com.wolterskluwer.service.mime.MimeType;
import com.wolterskluwer.service.mime.MimeTypeUtil;
import com.wolterskluwer.service.util.ContentEncoder;

public class ClientRunner {

	private static final String DEFAULT_REPORT_TEMPLATE_LOCATION = "conf/test-result-template.xhtml";

	private static final Logger logger = LoggerFactory.getLogger(ClientRunner.class);

	private static Options options;

	private static ValidationOperationSet validationOperationSet;

    static {
        GenericXmlApplicationContext context = new GenericXmlApplicationContext("context.xml");
        ContentODataClient client = context.getAutowireCapableBeanFactory().getBean(ContentODataClient.class);
        validationOperationSet = client.getValidationOperationSet();
    }

	/**
	 * The main method starts the console client.
	 * 
	 * @param arguments
	 */
	public static void main(String[] arguments) {
		runMain(new ClientRunner(), arguments);
	}

	public static void runMain(ClientRunner clientRunner, String[] arguments) {
		options = new Options();
		CmdLineParser parser = new CmdLineParser(options);
		try {
			parser.parseArgument(arguments);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("Proper arguments to run validation:");
			parser.printUsage(System.err);
			System.exit(1);
		}

		try {
			List<ProcessResult> processResultsList = null;
			Map<String, InputParameter> idToInputParameterMap = mapRandomIdToEachInputParameter(options.getInput());
			ContentJob job = getContentJob(idToInputParameterMap);
			if (options.isAsynch()) {
				logger.info("Calling async validation.");
				UUID validationId = sendValidateAsynchRequest(job);
				ProcessStatus status = waitAndTrackStatus(validationId);
				processResultsList = getResults(validationId, status);
			} else {
				logger.info("Calling sync validation.");
				Validate validateRequest = new Validate();
				validateRequest.setJob(job);
				ValidateResponse result = validationOperationSet.validate(validateRequest);
				processResultsList = result.getResult();
			}
			logger.info("Done.");

			for (ProcessResult result : processResultsList) {
				Report report = result.getReport();
				List<Message> messages = report.getMessage();
				boolean failed = hasErrors(messages);
				InputParameter input = idToInputParameterMap.get(result.getSourceContentObjectID());
				String inputStr = input.getData();
				if (failed) {
					logger.info(String.format("Validation of '%s' is failed.", inputStr));
				} else {
					logger.info(String.format("Validation of '%s' is successful.", inputStr));
				}
				//restoring original input names
				result.setSourceContentObjectID(inputStr);
			}
			
			
			createReportFile(processResultsList);
		} catch (Exception e) {
			logger.error("Client call failed.", e);
		}
	}

	/**
	 * Maps each item from the given array to <code>InputParameter</code>s to a random string ID.
	 * 
	 * @param inputParameters a list of <code>InputParameter</code>s to generate ID for each entry
	 * @return a map containing all <code>InputParameter</code>s with randomly generated IDs as the
	 * keys 
	 */
	private static Map<String, InputParameter> mapRandomIdToEachInputParameter(
	        List<InputParameter> inputParameters) {
		Map<String, InputParameter> result = new HashMap<String, InputParameter>();
		for(InputParameter inputParameter: inputParameters){
			result.put(UUID.randomUUID().toString(), inputParameter);
		}
		return result;
	}

	private static PlanIdentifier getPlanIdentifier() {
		PlanIdentifier planIdentifier = new PlanIdentifier();
		planIdentifier.setName(options.getPlanName());
		planIdentifier.setVersion(options.getPlanVersion());
		planIdentifier.setContentService(ContentService.VALIDATION);
		return planIdentifier;
	}

	private static UUID sendValidateAsynchRequest(ContentJob job)
			throws Exception {

		ValidateAsynch request = new ValidateAsynch();
		request.setJob(job);

		return validationOperationSet.validateAsynch(request);
	}

	private static ContentJob getContentJob(Map<String, InputParameter> idToInputParameterMap) throws IOException {
		ContentJob contentJob = new ContentJob();
		contentJob.setPlanId(getPlanIdentifier());
		setProperties(contentJob);
		List<ContentObject> contentObjects = new ArrayList<ContentObject>();

		for (String id : idToInputParameterMap.keySet()) {
			ContentObject contentObject = createContentObject(id, idToInputParameterMap.get(id));
			if (contentObject != null) {
				contentObjects.add(contentObject);
			}
		}
		if (contentObjects.isEmpty()) {
			logger.error("You have to define at least one path to input file. MIME-type is required.");
			System.exit(1);
		}
		contentJob.setContentObject(contentObjects);
		return contentJob;
	}

	private static ContentObject createContentObject(String id, InputParameter inputParameter)
	        throws IOException {

		String mimeTypeString = getMimeType(inputParameter, options.getMimeType());
		if (mimeTypeString == null) {
			logger.warn("Parameter is ignored because it has no MIME-type: " + inputParameter.getData());
			return null;
		}

		MimeType mimeType = MimeType.parse(mimeTypeString);
		if (options.isGzip()) {
		    mimeType = MimeTypeUtil.copyAndSetGzipEnabled(mimeType, true);
		}

		String data = inputParameter.getData();
		File file = getInputFile(data);
		String content = prepareContent(file, mimeType);
		return createContentObject(id, content, mimeType.toString());
	}

	private static ContentObject createContentObject(String id, String inputUrl, String mimeType) {
	    ObjectData objectData = new ObjectData();
	    objectData.setData(inputUrl);
	    objectData.setMimeType(mimeType);

		ContentObject result = new ContentObject();
		result.setId(id);
		result.setContent(objectData);
		return result;
	}

	/**
	 * Retrieves the MIME-type for the given input parameter. 
	 * 
	 * @param inputParameter
	 * @param defaultMimeType
	 * @return
	 */
	private static String getMimeType(InputParameter inputParameter, String defaultMimeType) {
		String mimeType = inputParameter.getMimeType();
		return mimeType != null ? mimeType : defaultMimeType;
	}

	private static String prepareContent(File file, MimeType mimeType) throws IOException {
	    if (MimeTypeUtil.isUrl(mimeType)) {
	        return toUrl(file);
	    }
	    byte[] data = FileUtils.readFileToByteArray(file); 
	    return ContentEncoder.encode(data, mimeType);
	}

    private static String toUrl(File file) {
        String result = file.toURI().toString();
        if (!result.startsWith("file:///")) {
            result = result.replaceAll("file:/+", "file:///");
        }
        return result;
    }

	private static File getInputFile(String inputUrl) {
		if (inputUrl.startsWith("file:/")) {
			URI uri = URI.create(inputUrl);
			return new File(uri);
		} else {
			return new File(inputUrl);
		}
	}

	private static void setProperties(ContentJob job) {
		List<com.wolterskluwer.osa.content.odata.api.Property> properties = 
				new ArrayList<com.wolterskluwer.osa.content.odata.api.Property>();
		for (Property prop : options.getProperties()) {
			com.wolterskluwer.osa.content.odata.api.Property property =
			        new com.wolterskluwer.osa.content.odata.api.Property();
			property.setName(prop.getName());
			property.setValue(prop.getValue());
			properties.add(property);
		}
		job.setProperty(properties);
	}

	private static List<ProcessResult> getResults(UUID validationId, ProcessStatus status)
	        throws Exception {
		GetValidationResult request = new GetValidationResult();
		request.setProcessId(validationId);
		GetValidationResultResponse response = validationOperationSet.getValidationResult(request);
		return response.getResult();
	}

	private static void createReportFile(List<ProcessResult> results) throws Exception {
		File reportLocation = Files.getResultFile(options.getReport(), "report", "report.html");
		VelocityUtil.applyTemplate(results, DEFAULT_REPORT_TEMPLATE_LOCATION, reportLocation);
	}

	private static boolean hasErrors(List<Message> messages) {
		if (messages != null) {
			for (Message message : messages) {
				if (message.getStatus() == Status.ERROR) {
					return true;
				}
			}
		}
		return false;
	}

	private static ProcessStatus waitAndTrackStatus(UUID validationId) throws Exception {
		logger.info("Validation started. Job ID is " + validationId);
		for (;;) {
			ProcessStatus status = requestValidationStatus(validationId);
			logger.info("Current validation status: " + status);

			if (status == ProcessStatus.FINISHED || status == ProcessStatus.FINISHED_ERROR) {
				return status;
			}

			synchronized (ClientRunner.class) {
				ClientRunner.class.wait(3000);
			}
		}
	}

	private static ProcessStatus requestValidationStatus(UUID validationId) throws Exception {
		GetValidationStatus request = new GetValidationStatus();
		request.setProcessId(validationId);
		return validationOperationSet.getValidationStatus(request);
	}
}
