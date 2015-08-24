package com.wolterskluwer.service.content.validation.util;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class URIUtils {

	public static Map<String, String> getUriParameters(URI uri) {
		String query = uri.getQuery();
		if (query == null) {
			return Collections.emptyMap();
		}
		return parseQueryParameters(query);
	}

	static Map<String,String> parseQueryParameters(String query) {
		String[] rawParameters = query.split("&"); 
		HashMap<String, String> parameters = new HashMap<String, String>(rawParameters.length); 
		for (String rawParameter : rawParameters) {
			int parameterSeparatorIndex = rawParameter.indexOf('=');
			String parameterName;
			String parameterValue;
			if (parameterSeparatorIndex == -1) {
				parameterName = rawParameter.substring(0);
				parameterValue = ""; 
			} else {
				parameterName = rawParameter.substring(0, parameterSeparatorIndex);
				parameterValue = rawParameter.substring(parameterSeparatorIndex + 1);
			}
			parameters.put(parameterName, parameterValue);
		}
		return parameters;
	}

	public static String getUriWithoutQuery(URI uri) {
		String uriString = uri.toASCIIString();
		String query = uri.getRawQuery();
		int queryLength = query == null ? 0 : query.length() + 1; // the query length including "?" 
		int queryStartIndex = uriString.length() - queryLength;
		return uriString.substring(0, queryStartIndex);
	}
}
