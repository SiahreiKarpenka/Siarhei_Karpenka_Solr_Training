package com.wolterskluwer.service.content.validation.util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class XPathSelector {

    private static final XPathFactory xpathFactory = XPathFactory.newInstance();

    private final XPath xpath;
    private final Document document;
    private final DocumentNamespaceContext documentNamespaceContext;

    public static XPathSelector forDocument(Document document) {
        return new XPathSelector(document);
    }

    private XPathSelector(Document document) {
        this.document = document;
        this.documentNamespaceContext = new DocumentNamespaceContext(document);
        this.xpath = createXPath(document, documentNamespaceContext);
    }

    public void declareNamespace(String prefix, String namespaceURI) {
        documentNamespaceContext.addNamespace(prefix, namespaceURI);
    }

    private XPath createXPath(Document document, DocumentNamespaceContext documentNamespaceContext) {
        XPath xpath = xpathFactory.newXPath();
        xpath.setNamespaceContext(documentNamespaceContext);
        return xpath;
    }

    public NodeList queryNodes(String expression) throws XPathExpressionException {
        return (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
    }

    public Node queryNode(String expression) throws XPathExpressionException {
        return (Node) xpath.evaluate(expression, document, XPathConstants.NODE);
    }

    public String queryString(String expression) throws XPathExpressionException {
        return (String) xpath.evaluate(expression, document, XPathConstants.STRING);
    }
}
