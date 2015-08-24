package com.wolterskluwer.service.content.validation.util;

import org.w3c.dom.Document;

import javax.xml.namespace.NamespaceContext;

import java.util.HashMap;
import java.util.Iterator;

/**
 * {@link NamespaceContext} implementation which looks up namespace information in the source
 * document instance.
 *
 */
public class DocumentNamespaceContext implements NamespaceContext {

    private final Document document;
    private final Namespaces namespaces;

    /**
     * Constructs DocumentNamespaceContext with the given <code>document</code> as the source.
     *
     * @param document must not be null; a {@link Document} instance which will be used to resolve
     *                 namespace information
     * @throws NullPointerException if the <code>document</code> argument is null
     */
    public DocumentNamespaceContext(Document document) {
        if (document == null) {
            throw new NullPointerException();
        }
        this.document = document;
        this.namespaces = new Namespaces();
    }

    private String lookupNamespaceURI(String prefix) {
        String namespaceURI = namespaces.getNamespaceURI(prefix);
        if (namespaceURI == null) {
            namespaceURI = document.lookupNamespaceURI(prefix);
            if (namespaceURI != null) {
                namespaces.put(prefix, namespaceURI);
            }
        }
        return namespaceURI;
    }

    private String lookupPrefix(String namespaceURI) {
        String prefix = namespaces.getPrefix(namespaceURI);
        if (prefix == null) {
            prefix = document.lookupNamespaceURI(namespaceURI);
            if (prefix != null) {
                namespaces.put(prefix, namespaceURI);
            }
        }
        return prefix;
    }

    public void addNamespace(String prefix, String namespaceURI) {
        namespaces.put(prefix, namespaceURI);
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return lookupNamespaceURI(prefix);
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return lookupPrefix(namespaceURI);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Iterator getPrefixes(String namespaceURI) {
        return null;
    }

    static class Namespaces {

        HashMap<String, String> prefix2uri = new HashMap<String, String>();
        HashMap<String, String> uri2prefix = new HashMap<String, String>();

        public String getNamespaceURI(String prefix) {
            return prefix2uri.get(prefix);
        }

        public String getPrefix(String namespaceURI) {
            return uri2prefix.get(namespaceURI);
        }

        public void put(String prefix, String namespaceURI) {
            prefix2uri.put(prefix, namespaceURI);
            uri2prefix.put(namespaceURI, prefix);
        }
    }
}
