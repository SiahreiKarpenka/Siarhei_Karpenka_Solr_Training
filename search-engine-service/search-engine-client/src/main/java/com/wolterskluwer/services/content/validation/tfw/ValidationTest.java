package com.wolterskluwer.services.content.validation.tfw;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.wolterskluwer.services.library.testing.beans.AbstractTest;


/**
 * Contains data that is used for testing a certain PCI package.
 * 
 */
public class ValidationTest extends AbstractTest {

    private static final String BEAN_PACKAGE = "validatePackage";
    private String packageURL = null;
    private String validationURI = null;

    private HashMap<String, String> inputParams = new HashMap<String, String>();

    public ValidationTest() {
    }

    public ValidationTest(String name, String description, String packageURL, String validationURI) {
        setName(name);
        setDescription(description);
        setPackageURL(packageURL);
        setValidationURI(validationURI);
        setBean(BEAN_PACKAGE);
    }

    public String getPackageURL() {
        return packageURL;
    }

    public void setPackageURL(String packageURL) {
        if (packageURL == null) {
            throw new NullPointerException("Could not create the test: the package URL is null.");
        }
        this.packageURL = packageURL;
    }

    public void addInputParameter(String name, String value) {
        this.inputParams.put(name, value);
    }

    public Map<String, String> getInputParameters() {
        return Collections.unmodifiableMap(inputParams);
    }

    public String getValidationURI() {
        return validationURI;
    }

    public void setValidationURI(String validationURI) {
        this.validationURI = validationURI;
    }
}
