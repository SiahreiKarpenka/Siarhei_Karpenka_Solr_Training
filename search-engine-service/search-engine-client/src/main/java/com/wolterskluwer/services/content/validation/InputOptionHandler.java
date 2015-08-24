package com.wolterskluwer.services.content.validation;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

public class InputOptionHandler extends OptionHandler<List<InputParameter>> {

	public InputOptionHandler(CmdLineParser parser, OptionDef option,
	        Setter<List<InputParameter>> setter) {
		super(parser, option, setter);
	}

	@Override
	public String getDefaultMetaVariable() {
		return "List<InputParameter>";
	}

	@Override
	public int parseArguments(Parameters parameters) throws CmdLineException {
		return parseInParameterArguments(parameters);
	}

	/**
	 * Parses arguments between -in and other argument starting with '-'
	 * 
	 * @param parameters
	 *            arguments that follow after -in (paths). Each of them can
	 *            contain either just a path or a path with parameters separated
	 *            by '?' (e.g. MIME-type)
	 * 
	 * @return
	 * 
	 * @throws CmdLineException
	 *             when there are no arguments specified between -in and other
	 *             argument named starting with '-'
	 */
	private int parseInParameterArguments(Parameters parameters) throws CmdLineException {
		ArrayList<InputParameter> inputParameters = new ArrayList<InputParameter>();

		int parametersSize = parameters.size();
		int parameterIndex = 0;

		for (; parameterIndex < parametersSize; parameterIndex++) {
			String param = parameters.getParameter(parameterIndex);

			if (param.startsWith("-")) {
				break;
			}

			InputParameter input = parseParameter(param);
			if (input != null) {
				inputParameters.add(input);
			}
		}

		if (inputParameters.isEmpty()) {
			throw new CmdLineException(super.owner,
					"You have to define at least one path to input file.");
		}
		this.setter.addValue(inputParameters);

		return parameterIndex;
	}

	static InputParameter parseParameter(String param) {
		String[] tokens = param.split("\\?");
		String path = tokens[0]; // never null
		String mimeType = null;

		if (path.trim().isEmpty()) {
			return null; // no path
		}
		if (tokens.length > 1) {
			mimeType = tokens[1];
		}

		return new InputParameter(tokens[0], mimeType);
	}
}
