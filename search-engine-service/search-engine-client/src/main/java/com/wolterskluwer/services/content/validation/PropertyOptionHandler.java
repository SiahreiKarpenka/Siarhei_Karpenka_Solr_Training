package com.wolterskluwer.services.content.validation;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyOptionHandler extends OptionHandler<List<Property>> {

	private static final Logger logger = LoggerFactory
			.getLogger(PropertyOptionHandler.class);

	public PropertyOptionHandler(CmdLineParser parser, OptionDef option,
			Setter<? super List<Property>> setter) {
		super(parser, option, setter);
	}

	@Override
	public String getDefaultMetaVariable() {
		return "List<Property>";
	}

	@Override
	public int parseArguments(Parameters parameters) throws CmdLineException {
		return parsePropertyParameterArguments(parameters);
	}

	/**
	 * Parses arguments between -p and other argument starting with '-'
	 * 
	 * @param parameters
	 *            arguments that follow after -p (properties). Each of them
	 *            contains name and value separated by '=' (e.g. log=info)
	 * 
	 * @return
	 * 
	 */
	private int parsePropertyParameterArguments(Parameters parameters)
			throws CmdLineException {
		ArrayList<Property> propertyParameters = new ArrayList<Property>();

		int parametersSize = parameters.size();
		int parameterIndex = 0;

		for (; parameterIndex < parametersSize; parameterIndex++) {
			String parameter = parameters.getParameter(parameterIndex);

			if (parameter.startsWith("-")) {
				break;
			}

			Property property = parseProperty(parameter);
			if (property != null) {
				propertyParameters.add(property);
			}
		}

		this.setter.addValue(propertyParameters);
		return parameterIndex;
	}

	static Property parseProperty(String parameter) {
		int index = parameter.indexOf("=");
		if (index < 0) {
			logger.warn(String.format("'%s' - no property value specified.",
					parameter));
			return null;
		} else if (index == 0) {
			logger.warn(String.format("'%s' - no property name specified.",
					parameter));
			return null;
		} else {
			String name = parameter.substring(0, index);
			String value = parameter.substring(index + 1);
			return new Property(name, value);
		}
	}
}