package com.wolterskluwer.service.content.validation.validator;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple XML parser based on DocumentBuilder with embedded catalog resolver functionality.
 * By default, XMLParser is configured as non-validating and namespace-aware.
 *
 * @see CatalogResolver
 */
public class XMLParser {

    private final DocumentBuilderFactory documentBuilderFactory;
    private final CatalogResolver resolver;
    private DocumentBuilder documentBuilder;

    /**
     * Constructs {@link XMLParser} instance without catalog resolver.
     */
    public XMLParser() {
        this(null);
    }

    /**
     * Constructs {@link XMLParser} with the given {@link CatalogResolver} instance.
     *
     * @param resolver a catalog entity resolver that will be used for resolving entities
     */
    XMLParser(CatalogResolver resolver) {
        this.resolver = resolver;
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        initDocumentBuilderFactory(documentBuilderFactory);
    }

    /**
     * Constructs {@link XMLParser} instance with the given catalog file which will be used to
     * resolve entities.
     *
     * @param filePath the path to the catalog file
     * @return {@link XMLParser} instance
     */
    public static XMLParser withCatalog(String filePath) {
        if (filePath == null) {
            return new XMLParser();
        } else {
            return new XMLParser(new CatalogResolver(filePath));
        }
    }

    /**
     * Initializes the passed {@link DocumentBuilderFactory} instance, configures it according to
     * the default settings.
     *
     * @param factory
     */
    private void initDocumentBuilderFactory(DocumentBuilderFactory factory) {
        factory.setValidating(false);
        factory.setNamespaceAware(true);
    }

    public void setValidating(boolean validating) {
        documentBuilderFactory.setValidating(validating);
    }

    public boolean isValidating() {
        return documentBuilderFactory.isValidating();
    }

    public Document parseXML(File file) throws IOException, SAXException {
        DocumentBuilder builder = getDocumentBuilder();
        return builder.parse(file);
    }

    public Document parseXML(InputStream in) throws IOException, SAXException {
        try {
            DocumentBuilder builder = getDocumentBuilder();
            return builder.parse(in);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private DocumentBuilder getDocumentBuilder() {
        if (documentBuilder == null) {
            documentBuilder = createDocumentBuilder();
        }
        return documentBuilder;
    }

    /**
     * Creates a {@link DocumentBuilder} instance which is responsible for parsing XML documents.
     * This method attaches the catalog resolver (specified in the constructor) to the newly created
     * {@link DocumentBuilder} instance.
     *
     * @return a {@link DocumentBuilder} instance with predefined catalog resolver
     */
    private DocumentBuilder createDocumentBuilder() {
        try {
            if (resolver == null) {
                documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            }
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            if (resolver != null) {
                builder.setEntityResolver(resolver);
            }
            return builder;
        } catch (ParserConfigurationException e) {
            // if this exception occurs, this class needs to be fixed; we DO NOT bring
            // this exception to client code
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
