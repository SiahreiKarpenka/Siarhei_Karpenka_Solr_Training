package com.wolterskluwer.services.content.validation;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.ExplicitBooleanOptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Options {

	private static final Logger logger = LoggerFactory.getLogger(Options.class);

	private String planName;
	
	private String planVersion;
	
	private String report;
	
	private List<InputParameter> input;
	
	private String mimeType;
	
	private List<Property> properties = new ArrayList<Property>();
	
	private boolean asynch;

	private boolean gzip;

	public List<InputParameter> getInput() {
		return input;
	}

	@Option(name = "-in", handler = InputOptionHandler.class, usage = "paths to input files", required = true)
	public void setInput(List<InputParameter> input) {
		this.input = input;
	}

	protected void warnIfNotEmpty(String oldValue, String newValue,
			String parameterName) {
		if (oldValue != null && !oldValue.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("-");
			message.append(parameterName);
			message.append(" parameter used multiple times. Using the latest value ");
			message.append(newValue);
			logger.warn(message.toString());
		}
	}

	public String getReport() {
		return report;
	}

	@Option(name = "-report", usage = "report file path", required = false)
	public void setReport(String report) {
		warnIfNotEmpty(this.report, report, "report");
		this.report = report;
	}
	
	public String getMimeType() {
		return mimeType;
	}
	
	@Option(name = "-mime-type", usage = "default mimetype", required = false)
	public void setMimeType(String mimeType) {
		warnIfNotEmpty(this.mimeType, mimeType, "mime-type");
		this.mimeType = mimeType;
	}

	public List<Property> getProperties() {
		return properties;
	}

	@Option(name = "-p", aliases = {"-property"}, handler = PropertyOptionHandler.class, usage = "properties list", required = false)
	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}
	
	public boolean isAsynch() {
		return asynch;
	}

	@Option(name = "-a", aliases = {"-asynch"}, handler = ExplicitBooleanOptionHandler.class, usage = "properties list", required = false)
	public void setAsynch(boolean asynch) {
		this.asynch = asynch;
	}
	public String getPlanName() {
		return planName;
	}

	@Option(name = "-plan-name", usage = "plan name", required = true, aliases = { "-pl" })
	public void setPlanName(String planName) {
		warnIfNotEmpty(this.planName, planName, "plan-name");
		this.planName = planName;
	}

	public String getPlanVersion() {
		return planVersion;
	}

	@Option(name = "-plan-version", usage = "plan version", required = true, aliases = { "-v" })
	public void setPlanVersion(String planVersion) {
		warnIfNotEmpty(this.planVersion, planVersion, "plan-version");
		this.planVersion = planVersion;
	}

    public boolean isGzip() {
        return gzip;
    }

    @Option(name = "-gzip", usage = "use gzip compression", required = false)
    public void setGzip(boolean gzip) {
        this.gzip = gzip;
    }
}