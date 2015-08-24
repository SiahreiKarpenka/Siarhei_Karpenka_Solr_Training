package com.wolterskluwer.services.content.validation;

import java.io.File;
import java.io.FileWriter;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.ToolContext;
import org.apache.velocity.tools.ToolManager;

public class VelocityUtil {

	public static void applyTemplate(Object data,
			String templateLocation, File reportLocation) throws Exception {
		VelocityEngine ve = new VelocityEngine();
		ve.init();
		Template t = ve.getTemplate(templateLocation);
		ToolManager manager = new ToolManager();
		ToolContext context = manager.createContext();
		context.put("data", data);
		FileWriter writer = new FileWriter(reportLocation);
		t.merge(context, writer);
		writer.close();
	}
}
