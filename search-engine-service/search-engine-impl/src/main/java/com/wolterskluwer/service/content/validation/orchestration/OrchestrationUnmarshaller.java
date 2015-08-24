package com.wolterskluwer.service.content.validation.orchestration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import com.wolterskluwer.service.content.validation.ValidationException;

public class OrchestrationUnmarshaller {

    public static final String ORCHESTRATION_PACKAGE_NAME
            = "com.wolterskluwer.service.content.validation.orchestration";

    public Orchestration unmarshall(File file) throws ValidationException {
        try {
            JAXBContext context = JAXBContext.newInstance(ORCHESTRATION_PACKAGE_NAME);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            JAXBElement<Orchestration> jaxbElement =
            		unmarshaller.unmarshal(new StreamSource(file), Orchestration.class);
            return jaxbElement.getValue();
        } catch (Exception e) {
            throw new ValidationException("Cannot unmarshall file " + file.getAbsolutePath(), e);
        }
    }

    public Orchestration unmarshallResolveParams(File file) throws ValidationException {
        Orchestration orchestration = unmarshall(file);
        ParamValueResolver resolver = new ParamValueResolver(orchestration,
        		Collections.<String,String>emptyMap());
        resolver.resolveParams();
        return orchestration;
    }

    public Orchestration unmarshallResolveParams(File file, Map<String,String> propertyOverride)
    		throws ValidationException {
        Orchestration orchestration = unmarshall(file);
        ParamValueResolver resolver = new ParamValueResolver(orchestration, propertyOverride);
        resolver.resolveParams();
        return orchestration;
    }

    private static class ParamValueResolver {

        private Orchestration orchestration;

        private PropertyContext context;

        private ParamValueResolver(Orchestration orchestration, Map<String, String> propertyOverride) {
            this.orchestration = orchestration;
            this.context = new PropertyContext();
            initProperties(propertyOverride);
        }

        private void initProperties(Map<String, String> propertyOverride) {
        	Map<String, String> properties = toPropertyMap(orchestration.getProperty());
        	properties.putAll(propertyOverride);
            for (String propertyKey : properties.keySet()) {
            	if (propertyKey != null) {
            		String propertyValue = properties.get(propertyKey);
                    context.declareProperty(propertyKey, propertyValue);
            	}
            }
        }

        private Map<String,String> toPropertyMap(List<Property> properties) {
        	HashMap<String, String> propertyMap = new HashMap<String, String>();
        	for (Property property : properties) {
        		String propertyName = property.getName();
      			propertyMap.put(propertyName, property.getValue());
        	}
        	return propertyMap;
        }

        public void resolveParams() {
        	resolveParamsReporter();
        	resolveParamsContext();
        }

        private void resolveParamsReporter() {
        	List<XmlReporter> reporters = orchestration.getReporter();
        	for (XmlReporter reporter : reporters) {
        		for (Param param : reporter.getParam()) {
                    resolveParam(param);
                }
        	}
        }

        private void resolveParamsContext() {
        	List<Context> contexts = orchestration.getContext();
            ArrayList<Validation> validations = new ArrayList<Validation>();
            for (Context context : contexts) {
                validations.addAll(getValidations(context));
            }
            for (Validation validation : validations) {
                for (Param param : validation.getParam()) {
                    resolveParam(param);
                }
            }
        }

        private List<Validation> getValidations(Context context) {
            return context.getValidation();
        }

        public void resolveParam(Param param) {
            param.setValue(resolveParamValue(param));
        }

        private String resolveParamValue(Param param) {
            return context.resolve(param.getValue());
        }
    }
}
