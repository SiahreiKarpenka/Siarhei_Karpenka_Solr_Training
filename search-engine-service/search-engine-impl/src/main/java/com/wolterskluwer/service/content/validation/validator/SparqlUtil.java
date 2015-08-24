package com.wolterskluwer.service.content.validation.validator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;


public class SparqlUtil {

    private static Logger LOGGER = LoggerFactory.getLogger(SparqlUtil.class);

    private static final String SPRING_BEAN_NS = "http://www.springframework.org/schema/beans";

    private static final String VALUE_ELEMENT = "value";

    private static DocumentBuilderFactory documentBuilderFactory;

    public static final String PREFIXES_FILE_PATH = "/prefixes.sparql";

    private static String sparqlPrefixes = "";

    static {
        importPrefixDeclarations();
        initDocumentBuilderFactory();
    }

    /**
     * Loads SPARQL prefix declarations from an external text file. These
     * prefix declarations will be available for each SPARQL query extracted
     * from the source XML file.
     *
     * @see {@link #PREFIXES_FILE_PATH}
     */
    static void importPrefixDeclarations() {
        InputStream in = RdfBusinessRuleValidator.class.getResourceAsStream(PREFIXES_FILE_PATH);
        try {
            sparqlPrefixes = IOUtils.toString(in);
        } catch (IOException e) {
            LOGGER.error("Could not load SPARQL prefix declarations ({})",
                    e.toString());
        }
    }

    public static List<String> extractQueriesFromFile(File file)
            throws IOException, SAXException, ParserConfigurationException {
        Document doc = parseXml(file);
        NodeList nodes = doc.getElementsByTagNameNS(SPRING_BEAN_NS, VALUE_ELEMENT);
        ArrayList<String> queries = new ArrayList<String>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            queries.add(node.getTextContent().trim());
        }
        return queries;
    }

    /**
     * Parses an XML file using non-validating parser.
     */
    static Document parseXml(File file) throws ParserConfigurationException,
            SAXException, IOException {
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        return builder.parse(file);
    }

    private static void initDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        documentBuilderFactory = factory;
    }

    static Query compileQuery(String text) {
        text = prependPrefixDeclarations(text);
        return QueryFactory.create(text, Syntax.syntaxARQ);
    }

    static String prependPrefixDeclarations(String text) {
        StringBuilder sb = new StringBuilder(sparqlPrefixes);
        sb.append(text);
        return sb.toString();
    }
    
    /**
     * Retrieves a string containing all SPARQL prefix declarations
     * used for SPARQL query building.
     */
    static String getSparqlPrefixes() {
        return sparqlPrefixes;
    }
}
