package com.wolterskluwer.service.content.validation.validator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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

public class ValidationQueries implements Iterable<Query> {

    private static Logger LOGGER = LoggerFactory.getLogger(ValidationQueries.class);

    public static final String PREFIXES_FILE_PATH = "/prefixes.sparql";

    private static String sparqlPrefixes;

    private XmlParser parser;

    private List<Query> queries;

    static {
        importPrefixDeclarations();
    }

    static void importPrefixDeclarations() {
        InputStream in = RdfBusinessRuleValidator.class.getResourceAsStream(PREFIXES_FILE_PATH);
        try {
            sparqlPrefixes = IOUtils.toString(in);
        } catch (IOException e) {
            LOGGER.error("Could not load SPARQL prefix declarations ({})", e.toString());
        }
    }

    public ValidationQueries() {
        queries = new ArrayList<Query>();
        parser = new XmlParser();
    }

    public void read(File file) throws IOException, SAXException {
        read(new FileInputStream(file));
    }

    public void read(InputStream in) throws IOException, SAXException {
        if (in == null) {
            throw new NullPointerException();
        }
        addQueries(extractQueries(in));
    }

    private List<String> extractQueries(InputStream in) throws IOException, SAXException {
        Document document = parseXml(in);
        return SparqlValidationDescriptor.extractQueriesFrom(document);
    }

    private void addQueries(List<String> textQueries) {
        for (String textQuery : textQueries) {
            queries.add(compileQuery(textQuery));
        }
    }

    private Document parseXml(InputStream in) throws IOException, SAXException {
        try {
            return parser.parse(in);
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException("The parser could not be configured.");
        }
    }

    public List<Query> asList() {
        return Collections.unmodifiableList(queries);
    }

    public int size() {
        return queries.size();
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

    @Override
    public Iterator<Query> iterator() {
        return queries.iterator();
    }

    private static class SparqlValidationDescriptor {

        private static final String SPRING_BEAN_NS = "http://www.springframework.org/schema/beans";
        private static final String VALUE_ELEMENT = "value";

        public static List<String> extractQueriesFrom(Document doc) {
            NodeList nodes = selectElementsIn(doc);
            ArrayList<String> queries = new ArrayList<String>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                queries.add(extractQueryText(node));
            }
            return queries;
        }

        private static String extractQueryText(Node node) {
            if (node == null) {
                return null;
            }
            String textContent = node.getTextContent();
            return normalizeText(textContent);
        }

        private static String normalizeText(String text) {
            return text.trim();
        }

        private static NodeList selectElementsIn(Document doc) {
            return doc.getElementsByTagNameNS(SPRING_BEAN_NS, VALUE_ELEMENT);
        }
    }

    private static class XmlParser {

        private DocumentBuilderFactory factory;

        private DocumentBuilder builder;

        public XmlParser() {
            factory = createDocumentBuilderFactory();
        }

        private DocumentBuilderFactory createDocumentBuilderFactory() {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            return factory;
        }

        private DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
            if (builder == null) {
                builder = factory.newDocumentBuilder();
            }
            return builder;
        }

        public Document parse(InputStream in) throws ParserConfigurationException, IOException, SAXException {
            return getDocumentBuilder().parse(in);
        }
    }
}
