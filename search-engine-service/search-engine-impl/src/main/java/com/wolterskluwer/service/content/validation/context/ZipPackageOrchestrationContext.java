package com.wolterskluwer.service.content.validation.context;

import com.wolterskluwer.csg.pci.api.PciApiException;
import com.wolterskluwer.csg.pci.api.v3.PciPackage;
import com.wolterskluwer.service.content.validation.*;
import com.wolterskluwer.service.content.validation.reporter.UpdatableReporter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ZipPackageOrchestrationContext extends OrchestrationContext {

	private static final String ZIP_POSTFIX = ".zip";

	private static final String[] INVALID_FILENAME_SYMBOLS = new String[] {
			"?", "=", "/" };

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ZipPackageOrchestrationContext.class);

	public ZipPackageOrchestrationContext(
			ValidationServiceConfiguration configuration, String id, ZipContentPack pack) {
		super(configuration);
		this.pack = pack;
		this.id = id;
	}

	private ZipContentPack pack;
	private String id;

	/**
	 * @return the pack
	 */
	public InputPack getPack() {
		return pack;
	}

	/**
	 * @param pack
	 *            the pack to set
	 */
	public void setPack(ZipContentPack pack) {
		this.pack = pack;
	}

	public List<String> getFiles(String mask, boolean caseSensetive)
			throws IOException {
		return pack.listRelFilePaths(mask, caseSensetive);
	}

	public List<String> getAllFiles() throws IOException {
		return pack.listRelFilePaths("**.*", false);
	}

	public String getSourceFilename() {
		return pack.getFileName();
	}

	public void cleanUp() throws IOException {
		pack.delete();
	}

	public InputStream getInputStream(String path) throws ValidationException {
		try {
			return this.pack.getInputStream(path);
		} catch (Exception e) {
			throw new ValidationException(e.getMessage(), e);
		}
	}

	public InputStream getInputStream() throws ValidationException {
		try {
			throw new NotImplementedException(
					"This method is not implemented for package.");
		} catch (Exception e) {
			throw new ValidationException(e.getMessage(), e);
		}
	}

	public String getInputFileName() {
		return pack.getName();
	}

	public String getInputFileNameWithExtention() {
		return pack.getFileName();
	}

	@Override
	public boolean isPackage() {
		return true;
	}

	@Override
	public void initReporter(UpdatableReporter reporter) {
		String packageName = prepareFilename(pack.getSourceUrl());
		reporter.setParameter("package", packageName);
		try {
			PciPackage pciPackage = new PciPackage(pack.getLocalFile());
			reporter.setParameter("package", pciPackage);
		} catch (PciApiException e) {
			LOGGER.warn("Error while reporting PCI package to Report Service",
					e.getMessage());
		}
	}

	public String prepareFilename(String packageURL) {
		String filename = packageURL;
		for (String special : INVALID_FILENAME_SYMBOLS) {
			int index = filename.lastIndexOf(special);
			if (index > -1) {
				filename = filename.substring(index + 1);
			}
		}
		if (!filename.toLowerCase().endsWith(ZIP_POSTFIX)) {
			filename += ZIP_POSTFIX;
		}
		return filename;
	}

	@Override
	public void validate() throws ValidationException {
		try {
			List<String> pathes = pack.listRelFilePaths("**.*", false);
			if (CollectionUtils.isEmpty(pathes)) {
				throw new IOException("Package is empty or is not valid Zip archive");
			}
		} catch (Exception e) {
			throw new ValidationException(
			    getInvalidZipArchiveMessage(id, e), e);
		}
	}

	private static String getInvalidZipArchiveMessage(String source, Exception e) {
		String messagePattern = Messages.getInstance().getMessage(
				"msg.pack.not.zip");
		return String.format(messagePattern, source, e.getMessage());
	}
}
