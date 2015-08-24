package com.wolterskluwer.service.content.validation.validator;

import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;
import com.wolterskluwer.service.content.validation.ConfigurationResourceAccessException;
import com.wolterskluwer.service.content.validation.Messages;
import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.reporter.Reporter;

public class RdfBusinessRuleValidator extends AbstractBusinessRuleValidator implements Validator {


    RdfBusinessRuleValidator(ValidationContext context) {
        super(context);
    }

    @Override
    protected void validateContent(String[] paths) throws ValidationException {
        Reporter reporter = getReporter();
        try {
            if (paths.length > 0) {
                for (String path : paths) {
                    setReporter(reporter.getResourceReporter(path));
                    validateModel(getRdfModel(path));
                }
            } else {
                validateModel(getRdfModel("local.file"));
            }
        } catch (ConfigurationResourceAccessException ex) {
            getReporter().error(ex.getMessage());
        }
    }

    private void validateModel(Model model) throws ValidationException {
        try {
            validateAgainstMergedModels(model);
            List<String> queries = getQueriesFromProperties();
            validateModel(model, queries);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            getReporter().error(Messages.getInstance().getMessage("msg.rdf.invalid.rules") + e.toString());
        }
    }

    private void validateAgainstMergedModels(Model model) throws ValidationException {
        if (isAdditionalModelsProvided()) {
            if(isValidReasonerName(this.getParams().getParam(PARAM_REASONER))) {
                List<Model> ontologyModels = getModelsFromPaths(this.getParams().getParam(PARAM_ADDITIONAL_MODEL).split(","));
                validateModelAgainstOntologies(createReasoner(this.getParams().getParam(PARAM_REASONER)), model, ontologyModels, getReporter());
            } else {
                throwExceptionAndLogError("msg.rdf.ontology.unknownReasoner", null, this.getParams().getParam(PARAM_REASONER));
            }
        }
    }

    private boolean isAdditionalModelsProvided() {
        return this.getParams().getParam(PARAM_ADDITIONAL_MODEL) != null;
    }

	@Override
	void addValidationError(String jenaError) {
		 getReporter().error(Messages.getInstance().getMessage("msg.rdf.invalid.rules") + jenaError);
	}
}
