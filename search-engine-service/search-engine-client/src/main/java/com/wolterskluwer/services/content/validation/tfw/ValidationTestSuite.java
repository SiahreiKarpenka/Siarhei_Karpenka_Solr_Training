package com.wolterskluwer.services.content.validation.tfw;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.wolterskluwer.services.library.testing.beans.AbstractTestSuite;
import com.wolterskluwer.services.library.xml.Elements;
import com.wolterskluwer.services.library.xml.XMLParser;

/**
 * Iterable collection of PCI package tests.
 */
public class ValidationTestSuite extends AbstractTestSuite {

    public ValidationTestSuite(File file) throws Exception {
        super(file);
    }
    
    public ValidationTestSuite() {
    }
    
    public void loadSuite(File file) throws Exception {
        XMLParser parser = new XMLParser();
        Document document = parser.parseDocument(file);
        Element docEl = document.getDocumentElement();

        Element[] testElements = Elements.getElements(docEl, "test");
        Element testElement;
        // find all "test" XML elements and create a test for each one
        for (int i = 0, count = testElements.length; i < count; i++) {

            testElement = testElements[i];
            ValidationTest test = new ValidationTest();
            test.setName(Elements.getAttribute(testElement, "name"));

            if (Elements.hasAttribute(testElement, "method")) {
                test.setBean(Elements.getAttribute(testElement, "method"));

                Element[] inputParams = Elements.getElements(testElement, "input-param");
                // check if we have input parameters
                if (inputParams != null) {
                    for (Element inputParam : inputParams) {
                        test.addInputParameter(Elements.getAttribute(inputParam, "name"),
                                Elements.getAttribute(inputParam, "value"));
                    }
                }
            } else {
                Element inputElement = Elements.getElement(testElement, "input");
                if (inputElement != null) {
                    test.setPackageURL(Elements.getAttribute(inputElement, "location"));
                }
            }

            Element descriptionElement = Elements.getElement(testElement, "description");
            if (descriptionElement != null) {
                test.setDescription(descriptionElement.getTextContent());
            }

            Element[] errorElements = Elements.getElements(testElement, "error");
            if (errorElements != null) {
                for (Element errorElement : errorElements) {
                    test.addExpectedErrorMessage(Elements.getAttribute(errorElement, "message"));
                }
            }
            addTest(test);
        }
    }
}
