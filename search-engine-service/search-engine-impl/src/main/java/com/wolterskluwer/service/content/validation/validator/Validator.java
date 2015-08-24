package com.wolterskluwer.service.content.validation.validator;

import com.wolterskluwer.service.content.validation.Params;
import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.context.OrchestrationContext;
import com.wolterskluwer.service.content.validation.reporter.Reporter;

public interface Validator {
	/**
	 * 
	 * @param context OrchestrationContext, that contains configuration and package\file for validation
	 * @param params
	 * @param reporter
	 * @param paths - Array of paths to files that should be validated. If context is not the package, but concrete file than path = "\"
	 * @throws ValidationException
	 */
    void validate(OrchestrationContext context, Params params, Reporter reporter, String... paths) throws ValidationException;
}
