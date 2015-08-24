package com.wolterskluwer.service.content.validation;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.wolterskluwer.framework.async.task.IncompleteTaskException;
import com.wolterskluwer.framework.async.task.SubmissionKey;
import com.wolterskluwer.framework.async.task.TaskFuture;
import com.wolterskluwer.framework.async.task.TaskStatus;
import com.wolterskluwer.framework.async.task.UnboundSubmissionKeyException;
import com.wolterskluwer.osa.commons.odata.server.operations.QueryHolder;
import com.wolterskluwer.osa.commons.odata.utils.ExceptionUtil;
import com.wolterskluwer.osa.content.odata.api.ContentJob;
import com.wolterskluwer.osa.content.odata.api.ContentObject;
import com.wolterskluwer.osa.content.odata.api.GetValidationResult;
import com.wolterskluwer.osa.content.odata.api.GetValidationResultResponse;
import com.wolterskluwer.osa.content.odata.api.GetValidationStatus;
import com.wolterskluwer.osa.content.odata.api.Message;
import com.wolterskluwer.osa.content.odata.api.Metrics;
import com.wolterskluwer.osa.content.odata.api.ObjectData;
import com.wolterskluwer.osa.content.odata.api.ObjectStatus;
import com.wolterskluwer.osa.content.odata.api.PlanIdentifier;
import com.wolterskluwer.osa.content.odata.api.ProcessResult;
import com.wolterskluwer.osa.content.odata.api.ProcessStatus;
import com.wolterskluwer.osa.content.odata.api.Property;
import com.wolterskluwer.osa.content.odata.api.Report;
import com.wolterskluwer.osa.content.odata.api.Status;
import com.wolterskluwer.osa.content.odata.api.Validate;
import com.wolterskluwer.osa.content.odata.api.ValidateAsynch;
import com.wolterskluwer.osa.content.odata.api.ValidateResponse;
import com.wolterskluwer.osa.content.odata.server.operation.adapter.ValidationOperationSetAdapter;
import com.wolterskluwer.service.content.validation.beans.ValidationItem;
import com.wolterskluwer.service.content.validation.util.MessagePriority;
import com.wolterskluwer.service.discovery.util.PlanAddress;
import com.wolterskluwer.service.mime.MimeType;

@Component
public class ContentValidationImpl extends ValidationOperationSetAdapter {
	
	private static Logger log = Logger.getLogger(ContentValidationImpl.class);

    private static final Map<TaskStatus, ProcessStatus> statusMapping;
    
    private static final Map<MessagePriority, Status> messageStatusMapping;

    static {
        statusMapping = new HashMap<>();
        statusMapping.put(TaskStatus.QUEUED, ProcessStatus.QUEUED);
        statusMapping.put(TaskStatus.RUNNING, ProcessStatus.RUNNING);
        statusMapping.put(TaskStatus.COMPLETE, ProcessStatus.FINISHED);
        statusMapping.put(TaskStatus.ERROR, ProcessStatus.FINISHED_ERROR);

        messageStatusMapping = new HashMap<>();
        messageStatusMapping.put(MessagePriority.INFO, Status.INFO);
        messageStatusMapping.put(MessagePriority.DEBUG, Status.DEBUG);
        messageStatusMapping.put(MessagePriority.ERROR, Status.ERROR);
        messageStatusMapping.put(MessagePriority.WARNING, Status.WARNING);
    }

	@Override
	public ValidateResponse validate(QueryHolder queryHolder, Validate request) {

        ValidationItem validationItem = turnToValidationItem(request.getJob());
        ValidateResponse response = new ValidateResponse();
        try {
            ValidationResult validationResult = new ValidationTask(validationItem).perform();
			List<ProcessResult> processResults = toProcessResults(validationResult);
			response.setResult(processResults);
        } catch (Exception e) {
			String errorMessage = "Validation process is failed: " + e.getMessage();
			log.error(errorMessage, e);
			throw ExceptionUtil.create(Response.Status.INTERNAL_SERVER_ERROR, errorMessage);
        }

        Metrics metrics = new Metrics();
        metrics.setFinish(new Date());
        response.setMetrics(metrics);
        return response;
	}

	@Override
	public UUID validateAsynch(QueryHolder queryHolder, ValidateAsynch request) {
		ContentJob contentJob = request.getJob();
		UUID processId = null;
		try {
			ValidationItem validationItem = turnToValidationItem(request.getJob());

			SubmissionKey submissionKey = App.getTaskService().submit(new ValidationTask(validationItem));
			processId = UUID.fromString(submissionKey.toString());
		} catch (Exception e) {
			String errorMessage = "Can not start validation for " + contentJob.getPlanId().getName();
			log.error(errorMessage, e);
			throw ExceptionUtil.create(Response.Status.INTERNAL_SERVER_ERROR, errorMessage);
		} 
	    return processId;
	}

	@Override
	public ProcessStatus getValidationStatus(QueryHolder queryHolder, GetValidationStatus request) {
		log.info(String.format("Trying to get status for process '%s'.", request.getProcessId()));
        UUID validationId = request.getProcessId();
        TaskStatus taskStatus = App.getTaskService().check(SubmissionKey.fromString(validationId.toString()));
        return statusMapping.get(taskStatus);
	}
	
    @Override
	public GetValidationResultResponse getValidationResult(QueryHolder queryHolder, GetValidationResult request) {

        UUID validationId = request.getProcessId();

        GetValidationResultResponse response = new GetValidationResultResponse();
        

        try {
            TaskFuture<ValidationResult> taskFuture =
                    App.getTaskService().take(SubmissionKey.fromString(validationId.toString()));
            if (taskFuture.getStatus() == TaskStatus.COMPLETE ||
                    taskFuture.getStatus() == TaskStatus.ERROR) {
    			List<ProcessResult> processResults = toProcessResults(taskFuture.getResult());
    			response.setResult(processResults);
            } else {
                // This method is not expected to be called by the service client if the 
                // getValidationStatus() method previously retrieved ProcessStatus.FINISHED_ERROR
                // NOTE (IS): here I assume that FINISHED_ERROR doesn't mean that there are only
                // expected validation errors, but rather service errors that should not happen in 
                // the normal flow.
    			String errorMessage = "Task is not finished correctly. Task status is " + taskFuture.getStatus();
    			log.error(errorMessage);
    			throw ExceptionUtil.create(Response.Status.INTERNAL_SERVER_ERROR, errorMessage);
            }
        } catch (IncompleteTaskException e) {
            // This method is not expected to be called by the service client if the 
            // getValidationStatus() method din't previously retrieve ProcessStatus.FINISHED
            String errorMessage = "Task is not complete";
			log.error(errorMessage);
			throw ExceptionUtil.create(Response.Status.INTERNAL_SERVER_ERROR, errorMessage);
        } catch (UnboundSubmissionKeyException e) {
            String errorMessage = "Cannot find the task with ID " + validationId;
			log.error(errorMessage);
			throw ExceptionUtil.create(Response.Status.INTERNAL_SERVER_ERROR, errorMessage);
        }

        Metrics metrics = new Metrics();
        metrics.setFinish(new Date());
        response.setMetrics(metrics);
        return response;
	}

	private List<ProcessResult> toProcessResults(ValidationResult validationResult) {
		List<ProcessResult> processResults = new ArrayList<ProcessResult>();
		for(ContentObjectValidationResult result: validationResult.getResults()){
			ProcessResult processResult = toProcessResult(result);
			processResults.add(processResult);
		}
		return processResults;
	}
	
	private ProcessResult toProcessResult(ContentObjectValidationResult result) {
		ProcessResult processResult = new ProcessResult();
		Report report = new Report();
		processResult.setReport(report);
		processResult.setSourceContentObjectID(result.getSourceId());
		processResult.setStatus(getObjectStatus(result.getMessages()));
		report.setMessage(toOsaMessages(result.getMessages()));
		return processResult;
	}

    private List<Message> toOsaMessages(
			List<com.wolterskluwer.service.content.validation.util.Message> messages) {
    	List<Message> result = new ArrayList<>();
		for (com.wolterskluwer.service.content.validation.util.Message msg : messages) {
			Status osaStatus = toOsaStatus(msg.getStatus());
			if (osaStatus != null) {
				Message message = new Message();
				message.setStatus(osaStatus);
				message.setText(msg.getText());
				message.setSubject(msg.getSubject());
				result.add(message);
			}
		}
		return result;
	}

	private Status toOsaStatus(MessagePriority status) {
		return messageStatusMapping.get(status);
	}

    private ValidationItem turnToValidationItem(ContentJob job) {
        PlanIdentifier planId = job.getPlanId();
		PlanAddress planAddress = new PlanAddress(App.SERVICE_NAME, planId.getName(), planId.getVersion());

        ValidationItem validationItem = new ValidationItem();
        validationItem.setPlanAddress(planAddress);
        validationItem.setInputs(toInputContentObjectList(job.getContentObject()));
        setProperties(validationItem, job.getProperty());
        return validationItem;
    }
    
	private static List<InputContentObject> toInputContentObjectList(
	        List<ContentObject> contentObjects) {
        List<InputContentObject> inputContentObjects = new ArrayList<>();

        for (ContentObject contentObject : contentObjects) {
            String id = contentObject.getId();

            inputContentObjects.add(createInputContentObject(id, contentObject.getContent()));

            if (contentObject.getMetadata() != null) {
                inputContentObjects.add(createInputContentObject(id, contentObject.getMetadata()));
            }
        }

        return inputContentObjects;
    }

    private static InputContentObject createInputContentObject(String id, ObjectData objectData) {
        MimeType type = MimeType.parse(objectData.getMimeType());

        return new InputContentObject(id, type, objectData.getData());
    }

	private void setProperties(ValidationItem validationItem, List<Property> properties) {
		Map<String, String> result = new HashMap<>();
		if (properties != null) {
			for (Property prop : properties) {
				result.put(prop.getName(), prop.getValue());
			}			
		}
		validationItem.setProperties(result);
	}
	
	private ObjectStatus getObjectStatus(
			List<com.wolterskluwer.service.content.validation.util.Message> messages) {
		ObjectStatus result = ObjectStatus.OK;
		for (com.wolterskluwer.service.content.validation.util.Message message : messages) {
			if (message.getStatus() == MessagePriority.ERROR) {
				return ObjectStatus.ERROR;
			} else if (message.getStatus() == MessagePriority.WARNING) {
				result = ObjectStatus.WARNING;
			}
		}
		return result;
	}
}
