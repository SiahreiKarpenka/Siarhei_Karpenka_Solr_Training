package com.wolterskluwer.service.content.validation;

import java.util.ArrayList;
import java.util.List;

import com.wolterskluwer.framework.async.task.TaskResult;

public class ValidationResult implements TaskResult {

	private List<ContentObjectValidationResult> results = new ArrayList<ContentObjectValidationResult>();

	/**
	 * @return the results
	 */
	public List<ContentObjectValidationResult> getResults() {
		return results;
	}

	/**
	 * @param results
	 *            the results to set
	 */
	public void setResults(List<ContentObjectValidationResult> results) {
		this.results = results;
	}

	}
