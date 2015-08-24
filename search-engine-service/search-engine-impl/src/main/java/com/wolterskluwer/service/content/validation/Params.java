package com.wolterskluwer.service.content.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that represents orchestration.xml parameters. Could store array of
 * parameters with the same name
 */
public class Params {

    private Map<String, String[]> map = null;

    public Params() {
        this.map = new HashMap<String, String[]>();
    }

    Params(Map<String, String[]> map) {
        this.map = map;
    }

    public static Params fromMap(Map<String, String[]> map) {
        return new Params(map);
    }

    /**
     * @param name name of the property
     * @return null if property was not found, first element from the available
     *         values in other case
     */
    public String getParam(String name) {
        String[] values = map.get(name);
        if (values == null || values.length == 0) {
            return null;
        }
        return values[0];
    }

    /**
     * @param name - name of the property
     * @return returns String array of values. Could return null if there were no such property
     */
    public String[] getParams(String name) {
        return map.get(name);
    }

    public void addParam(String name, String value) {
    	if (getParams(name) != null) {
    		List<String> values = new ArrayList<String>(Arrays.asList(getParams(name)));
    		values.add(value);
    		map.put(name, values.toArray(new String[values.size()]));
    	} else {
    		map.put(name, new String[] {value});
    	}
    }
    
    public void setParam(String name, String value) {
        map.put(name, new String[] {value});
    }

    public void setParams(String name, String[] values) {
        map.put(name, values);
    }
}
