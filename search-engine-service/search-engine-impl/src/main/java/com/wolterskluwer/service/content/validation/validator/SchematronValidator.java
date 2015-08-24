package com.wolterskluwer.service.content.validation.validator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.wolterskluwer.service.content.validation.ConfigurationResourceAccessException;
import com.wolterskluwer.service.content.validation.Messages;
import com.wolterskluwer.service.content.validation.Params;
import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.ValidationServiceConfiguration;
import com.wolterskluwer.service.content.validation.context.OrchestrationContext;
import com.wolterskluwer.service.content.validation.reporter.Reporter;

public class SchematronValidator implements Validator {

    private static final String WARNING_FLAG_VALUE = "warning";
    private static final String FLAG_ATTRIBUTE = "flag";
    private static final String FAILED_ASSERT_NAME = "failed-assert";
    private static final String LOCATION_ATTR_NAME = "location";
    private static final String SCHEMATRON_SVRL_NS = "http://purl.oclc.org/dsdl/svrl";
    private static final String FAILURE_MESSAGE_PATTERN = "Assertion failed at %s %s";

    static final String PARAM_PATH_CATALOG = "path.catalog";
    static final String PARAM_PATH_SCHEMATRON = "path.schematron";

    private final TransformerFactory transformerFactory = createTransformerFactory();
    private Reporter reporter = null;
    private XMLParser xmlParser = null;

    SchematronValidator() {
    }

    private static TransformerFactory createTransformerFactory() {
        return TransformerFactory.newInstance();
    }

    static NodeList findFailedNodes(Element rootElement) {
        NodeList failedNodes = rootElement.getElementsByTagNameNS(SCHEMATRON_SVRL_NS,
                FAILED_ASSERT_NAME);
        return failedNodes;
    }

    void initReporter(Reporter reporter) {
        this.reporter = reporter;
    }

    void initXMLParser(String catalogFilePath) {
        xmlParser = XMLParser.withCatalog(catalogFilePath);
    }

    private Reporter getReporter() {
        if (reporter == null) {
            throw new IllegalStateException("The reporter is null.",new NullPointerException());
        }
        return reporter;
    }
    
	@Override
	public void validate(OrchestrationContext context, Params params, Reporter reporter, String... paths)
			throws ValidationException {
		initReporter(reporter);
        try {
            initXMLParser(getCatalogFilePath(context.getConfiguration(), params));
            File[] schmFiles = getSchematronFiles(context.getConfiguration(), params);
            if (paths.length > 0) {
                for (String path : paths) {
                    this.reporter = reporter.getResourceReporter(path);
                    validateInputStream(context.getInputStream(path), schmFiles);
                }
			} else if (!context.isPackage()) {
				validateInputStream(context.getInputStream(), schmFiles);
			}
        } catch (ConfigurationResourceAccessException ex) {
            reporter.error(ex.getMessage());
        }
    }

	private void validateInputStream(InputStream inputStream, File[] schmFiles) {
        try {
            for (File schmFile : schmFiles) {
                transformAndValidate(inputStream, schmFile);
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
	}

    private File[] getSchematronFiles(ValidationServiceConfiguration configuration,
                                      Params params) throws ValidationException {
        String value = params.getParam(PARAM_PATH_SCHEMATRON);
        if (value != null && !"".equals(value)) {
            File schematron = configuration.getFileResource(value);
            return new File[]{schematron};
        }
        return new File[0];
    }

    private String getCatalogFilePath(ValidationServiceConfiguration configuration,
                                      Params params) throws ValidationException {
        String catalogFilePath = params.getParam(PARAM_PATH_CATALOG);
        if (catalogFilePath != null && !"".equals(catalogFilePath)) {
            return configuration.getFileResource(catalogFilePath).getAbsolutePath();
        } else {
            return null;
        }
    }

    void transformAndValidate(InputStream inputStream, File schematronFile) {
        try {
            Source source = getSource(inputStream);
            StreamSource schematronSource = new StreamSource(schematronFile);
            DOMResult result = new DOMResult();
            Transformer transformer = transformerFactory.newTransformer(schematronSource);
            transformer.transform(source, result);
            analyzeTransformResult(result);
        } catch (Exception e) {
            getReporter().error(Messages.getInstance().getMessage("msg.xhtml.invalid.schematron") + e.getMessage());
        }
    }

    private Source getSource(InputStream inputStream) throws ParserConfigurationException, IOException,
            SAXException {
        Document document = xmlParser.parseXML(inputStream);
        return new DOMSource(document);
    }

    private void analyzeTransformResult(DOMResult result) {
        Element rootElement = (Element) result.getNode().getFirstChild();
        NodeList failedNodes = findFailedNodes(rootElement);
        final int FAILED_NODES_COUNT = failedNodes.getLength();
        for (int i = 0; i < FAILED_NODES_COUNT; i++) {
            Element failedElement = (Element) failedNodes.item(i);
            String flagValue = failedElement.getAttribute(FLAG_ATTRIBUTE);
            boolean warning = WARNING_FLAG_VALUE.equals(flagValue);
            String location = failedElement.getAttribute(LOCATION_ATTR_NAME);
            String message = getFormattedTextContent(failedElement);
            if(warning){
                reportWarning(FAILURE_MESSAGE_PATTERN, location, message);
            } else {
                reportError(FAILURE_MESSAGE_PATTERN, location, message);
            }
        }
    }

    private String getFormattedTextContent(Element element) {
        if (element == null) {
            return "";
        }
        String textContent = element.getTextContent();
        if (textContent == null) {
            return "";
        }
        textContent = textContent.trim();
        textContent = textContent.replaceAll("[\n\r\t]+", " ");
        return textContent;
    }

    void reportError(String pattern, Object... strings) {
        getReporter().error(generateMessage(pattern, strings));
    }

    public String generateMessage(String pattern, Object... strings) {
        return Messages.getInstance().getMessage("msg.xhtml.invalid.schematron")
                + String.format(pattern, strings);
    }

    void reportWarning(String pattern, Object... strings) {
        getReporter().warn(generateMessage(pattern, strings));
    }
}
