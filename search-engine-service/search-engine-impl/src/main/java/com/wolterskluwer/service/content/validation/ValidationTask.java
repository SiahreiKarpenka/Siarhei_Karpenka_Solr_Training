package com.wolterskluwer.service.content.validation;

import java.util.ArrayList;
import java.util.List;

import com.wolterskluwer.framework.async.task.Task;
import com.wolterskluwer.service.content.validation.beans.ValidationItem;
import com.wolterskluwer.service.content.validation.util.Message;
import com.wolterskluwer.service.content.validation.util.MessagePriority;
import com.wolterskluwer.service.mime.MimeType;

public class ValidationTask implements Task<ValidationResult> {

    private final ValidationItem validationItem;

    private boolean exceptional = false;

    public ValidationTask(ValidationItem validationItem) {
        this.validationItem = validationItem;
    }

    @Override
    public ValidationResult perform() {
        ValidationResult validationResult = new ValidationResult();
        try {
            ValidationServiceConfiguration configuration = 
                    App.getConfiguration(validationItem.getPlanAddress());
            performValidation(validationResult, configuration);
        } catch (Exception e) {
            validationResult = buildValidationResult(e.getMessage());
            exceptional = true;
        }
        return validationResult;
    }

    private void performValidation(ValidationResult validationResult,
            ValidationServiceConfiguration configuration) {
        for (InputContentObject inputContentObject : validationItem.getInputContentObjects()) {
            List<Message> messages = new ArrayList<Message>();
            try {
                ValidationReport report = validateContentObject(inputContentObject, configuration);
                messages.addAll(report.getMessages());
            } catch (Exception e) {
                messages.add(createErrorMessage(e.getMessage()));
            }
            ContentObjectValidationResult result = createResultEntry(messages, inputContentObject.getId());
            validationResult.getResults().add(result);
        }
    }

    private ValidationReport validateContentObject(InputContentObject inputContentObject,
            ValidationServiceConfiguration configuration) throws Exception {
        MimeType mimeType = inputContentObject.getMimeType();
        OrchestrationExecutor executor = new OrchestrationExecutor(
                configuration, validationItem.getProperties(),
                inputContentObject.getId(), inputContentObject.getData(),
                mimeType);
        return executor.performValidation();
    }

    private ContentObjectValidationResult createResultEntry(List<Message> messages, String inputId) {
        ContentObjectValidationResult resultEntry = new ContentObjectValidationResult();
        resultEntry.setSourceId(inputId);
        resultEntry.setMessages(messages);
        return resultEntry;
    }

    private ValidationResult buildValidationResult(String errorMessage) {
        ValidationResult validationResult = new ValidationResult();
        List<Message> messages = new ArrayList<Message>();
        messages.add(createErrorMessage(errorMessage));
        for (InputContentObject input : validationItem.getInputContentObjects()) {
            ContentObjectValidationResult resultEntry = createResultEntry(messages, input.getId());
            validationResult.getResults().add(resultEntry);
        }
        return validationResult;
    }

    private Message createErrorMessage(String message) {
        return new Message(message, null, MessagePriority.ERROR);
    }

    @Override
    public boolean isExceptional() {
        return exceptional;
    }
}