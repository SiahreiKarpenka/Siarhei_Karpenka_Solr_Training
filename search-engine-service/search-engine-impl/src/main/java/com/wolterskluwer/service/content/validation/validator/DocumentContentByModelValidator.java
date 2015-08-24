package com.wolterskluwer.service.content.validation.validator;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.wolterskluwer.service.content.validation.Params;
import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.context.OrchestrationContext;
import com.wolterskluwer.service.content.validation.reporter.Reporter;

public class DocumentContentByModelValidator implements Validator {

    private static final String EXTRACT_TEXT_XPATH = "extract.text.xpath";
    private static final String EXTRACT_TEXT_MODIFIER = "extract.text.regexp.modifier";
    private static final String EXTRACT_TEXT_MODIFIER_GROUP = "extract.text.regexp.modifier.group";
    private static final String ERROR_LABEL_PATTERN = "error.label.pattern";
    private static final String MODEL_FILE_PATH = "path.model.file";
    private static final String EXIST_QUERY_PATTERN = "exist.query.pattern";

    private OrchestrationContext context;
    private String modelFilePath;
    private XMLParser xmlParser = new XMLParser();
    private String existQueryPattern;
    private XPathExpression xpathExpression;
    private String errorPattern;
    private String regex;
    private int group = 1;

    protected void init(OrchestrationContext context, Params params)
            throws XPathExpressionException {
        this.context = context;
        this.existQueryPattern = params.getParam(EXIST_QUERY_PATTERN);
        this.modelFilePath = params.getParam(MODEL_FILE_PATH);
        this.errorPattern = params.getParam(ERROR_LABEL_PATTERN);
        this.regex = params.getParam(EXTRACT_TEXT_MODIFIER);
        String groupStr = params.getParam(EXTRACT_TEXT_MODIFIER_GROUP);
        if (groupStr != null) {
            group = Integer.parseInt(groupStr);
        }
        XPath xpath = XPathFactory.newInstance().newXPath();
        String xpathStr = params.getParam(EXTRACT_TEXT_XPATH);
        this.xpathExpression = xpath.compile(xpathStr);
    }

    private static boolean isDefined(String path) {
        return path != null && !"".equals(path);
    }

    @Override
    public void validate(OrchestrationContext context, Params params, Reporter reporter,
            String... paths) throws ValidationException {
        try {
            init(context, params);
            File modelFile = context.getConfiguration().getFileResource(modelFilePath);
            Model model = getRdfModel(modelFile.toURI().toString());
            for (String inputXml : paths) {
                checkXmlContent(inputXml, model, reporter);
            }
        } catch (Exception e) {
            throw new ValidationException(String.format(
                    "Can't check '%s' content in the model by exist query'%s'.",
                    xpathExpression.toString(), existQueryPattern), e);
        }
    }

    private void checkXmlContent(String inputXmlPath, Model model, Reporter reporter)
            throws IOException, SAXException, ValidationException, XPathExpressionException {
        Document doc = xmlParser.parseXML(context.getInputStream(inputXmlPath));
        String contentValue = xpathExpression.evaluate(doc, XPathConstants.STRING).toString();
        if (isDefined(contentValue)) {
            String modifiedValue = modifyByRegexp(contentValue);
            String query = String.format(existQueryPattern, modifiedValue);
            boolean exist = executeModelQuery(model, query);
            if (!exist) {
                reporter.getResourceReporter(inputXmlPath).error(
                        String.format(errorPattern, modifiedValue));
            }
        }
    }

    private String modifyByRegexp(String contentValue) {
        if (isDefined(regex)) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(contentValue);
            if (matcher.find()) {
                return matcher.group(group);
            }
        }
        return contentValue;
    }

    private boolean executeModelQuery(Model model, String queryString) {
        Query query = SparqlUtil.compileQuery(queryString);
        QueryExecution execution = QueryExecutionFactory.create(query, model);
        ResultSet set = execution.execSelect();

        return set.hasNext();
    }

    protected Model getRdfModel(String path) throws ValidationException {
        return context.getConfiguration().getModel(path);
    }
}
