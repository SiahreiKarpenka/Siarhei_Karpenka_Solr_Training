package com.wolterskluwer.service.content.validation.beans;

import java.util.List;
import java.util.Map;

import com.wolterskluwer.service.content.validation.InputContentObject;
import com.wolterskluwer.service.discovery.util.PlanAddress;

public class ValidationItem {

    private List<InputContentObject> inputs;

    private PlanAddress planAddress;

    private List<String> validationAspects;
    
    private Map<String, String> properties;

    public PlanAddress getPlanAddress() {
		return planAddress;
	}

	public void setPlanAddress(PlanAddress planAddress) {
		this.planAddress = planAddress;
	}

	public List<String> getValidationAspects() {
        return validationAspects;
    }

    public void setValidationAspects(List<String> validationAspects) {
        this.validationAspects = validationAspects;
    }

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public List<InputContentObject> getInputContentObjects() {
		return inputs;
	}

	public void setInputs(List<InputContentObject> inputs) {
		this.inputs = inputs;
	}

}
