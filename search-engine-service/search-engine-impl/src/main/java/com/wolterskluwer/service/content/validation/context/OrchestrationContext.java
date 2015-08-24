package com.wolterskluwer.service.content.validation.context;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.ValidationServiceConfiguration;
import com.wolterskluwer.service.content.validation.reporter.UpdatableReporter;

/**
 * This class represents the context of whole validation. It is uses by all
 * validators. It contains the nature of validation (file of package) as well as
 * configuration for validation. All validators are working with InputStream,
 * and InputStream could be retrieved from this context.
 * 
 */
public abstract class OrchestrationContext {

	/**
	 * configuration that will be used for validation
	 */
	private ValidationServiceConfiguration configuration;

	public OrchestrationContext(ValidationServiceConfiguration configuration) {
		this.configuration = configuration;
	}

	/**
	 * @return the configuration
	 */
	public ValidationServiceConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * @param configuration
	 *            the configuration to set
	 */
	public void setConfiguration(ValidationServiceConfiguration configuration) {
		this.configuration = configuration;
	}

	public abstract String getInputFileName();

	public abstract String getInputFileNameWithExtention();

	public abstract InputStream getInputStream() throws ValidationException;

	public abstract InputStream getInputStream(String path) throws ValidationException;

	public abstract void cleanUp() throws IOException;

	public abstract List<String> getAllFiles() throws IOException;

	public abstract List<String> getFiles(String mask, boolean caseSensetive)
			throws IOException;

	public abstract String getSourceFilename();

	public abstract boolean isPackage();

	public abstract void initReporter(UpdatableReporter reporter);
	
	public abstract void validate() throws ValidationException;
}
