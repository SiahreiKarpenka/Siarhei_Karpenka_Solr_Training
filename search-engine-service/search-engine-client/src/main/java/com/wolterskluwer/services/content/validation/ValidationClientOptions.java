package com.wolterskluwer.services.content.validation;

import java.util.List;

import org.kohsuke.args4j.Option;

import com.wolterskluwer.service.client.ServiceClientRunOptions;

public class ValidationClientOptions extends ServiceClientRunOptions {

	private String schema;
	
	private String mimeType;
	
	private List<Property> properties;

	public synchronized String getSchema() {
		return schema;
	}

	@Option(name="-schema", usage="path to schema for XML validation (optional parameter)")
	public void setSchema(String schema) {
		warnIfNotEmpty(this.schema, schema, "schema");
		this.schema = schema;
	}
	
	public String getMimeType() {
		return mimeType;
	}

	@Option(name = "-mime-type", required = true, usage = "input mime type.")
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
	
}
