package com.wolterskluwer.service.content.validation.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.arp.DOM2Model;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.wolterskluwer.service.content.validation.validator.CatalogResolver;

public class CatalogXmlModelParser implements XmlModelParser {

    private DocumentBuilder documentBuilder;

    private EntityResolver entityResolver;

    private ErrorHandler errorHandler;

    CatalogXmlModelParser() {
        documentBuilder = DBFactory.createDocumentBuilder();
    }

    CatalogXmlModelParser(EntityResolver resolver) {
        this();
        entityResolver = resolver;
    }

    public static CatalogXmlModelParser withCatalog(File file) {
        CatalogResolver catalogResolver = new CatalogResolver(file.getAbsolutePath());
        CatalogXmlModelParser instance = new CatalogXmlModelParser(catalogResolver);
        return instance;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public Model parseModel(InputStream in) {
        try {
            return readModel(in);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Model readModel(InputStream in) throws IOException, SAXException {
        Document srcDocument = parseDocument(in);
        Model model = ModelFactory.createDefaultModel();
        DOM2Model d2m = DOM2Model.createD2M(srcDocument.getBaseURI(), model);
        d2m.load(srcDocument);
        return model;
    }

    public Model readModel(File src) throws IOException, SAXException {
        Document srcDocument = parseDocument(new FileInputStream(src));
        Model model = ModelFactory.createDefaultModel();
        DOM2Model d2m = DOM2Model.createD2M(srcDocument.getBaseURI(), model);
        d2m.load(srcDocument);
        return model;
    }

    private Document parseDocument(InputStream in) throws IOException, SAXException {
        try {
            documentBuilder.reset();
            documentBuilder.setErrorHandler(errorHandler);
            documentBuilder.setEntityResolver(entityResolver);
            return documentBuilder.parse(new InputSource(in));
        } finally {
            IOUtils.closeQuietly(in);
        }
    }


    private static class DBFactory {

        public static DocumentBuilder createDocumentBuilder() {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            try {
                return factory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

}
