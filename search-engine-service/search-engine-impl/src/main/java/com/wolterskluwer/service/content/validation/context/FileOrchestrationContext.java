package com.wolterskluwer.service.content.validation.context;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.ValidationServiceConfiguration;
import com.wolterskluwer.service.content.validation.reporter.UpdatableReporter;

public class FileOrchestrationContext extends OrchestrationContext {

    private String id;

    private File singleFile;

	public FileOrchestrationContext(ValidationServiceConfiguration configuration, File file, String id) {
		super(configuration);
		this.id = id;
		this.singleFile = file;
	}

	/**
	 * @return the singleFile
	 */
	public File getSingleFile() {
		return singleFile;
	}

	/**
	 * @param singleFile
	 *            the singleFile to set
	 */
	public void setSingleFile(File singleFile) {
		this.singleFile = singleFile;
	}

	public List<String> getAllFiles() throws IOException {
		return Collections.singletonList(id != null ? id : singleFile.getPath());
	}

	public String getSourceFilename() {
		return singleFile.getName();
	}

	public void cleanUp() throws IOException {
		if (singleFile.exists()) {
			singleFile.delete();
		}
	}

	public InputStream getInputStream(String path) throws ValidationException {
		try {
			return new FileInputStream(singleFile);
		} catch (Exception e) {
			throw new ValidationException(e.getMessage(), e);
		}
	}

	public InputStream getInputStream() throws ValidationException {
		try {
			return new FileInputStream(singleFile);
		} catch (Exception e) {
			throw new ValidationException(e.getMessage(), e);
		}
	}

	public String getInputFileName() {
		return singleFile.getName();
	}

	public String getInputFileNameWithExtention() {
		return singleFile.getName();
	}

	@Override
	public List<String> getFiles(String mask, boolean caseSensetive) throws IOException {
		throw new NotImplementedException("Not implemented for file.");
	}

	@Override
	public boolean isPackage() {
		return false;
	}

	@Override
	public void initReporter(UpdatableReporter reporter) {
		// do nothing here
	}

	@Override
	public void validate() throws ValidationException {
		// do nothing here
	}
}
