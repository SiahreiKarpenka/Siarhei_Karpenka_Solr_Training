package com.wolterskluwer.service.content.validation.validator;

import org.apache.xerces.util.XMLCatalogResolver;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.io.IOException;

/**
 * A wrapper to {@link XMLCatalogResolver}. This class only reveals the implementation of
 * {@link EntityResolver} and {@link LSResourceResolver} interfaces. All method invocations are
 * delegated to the wrapped instance.
 */
public class CatalogResolver
        implements EntityResolver, LSResourceResolver {

    /**
     * Catalog resolver implementation used by this class as a base. This resolver already provides
     * the required functionality. All methods calls are delegated to this instance.
     */
    private final XMLCatalogResolver resolver;

    /**
     * Constructs {@link CatalogResolver} instance with the catalog located at the given file path.
     * The catalog XML file shouldn't refer to its external DTD or XSD.
     *
     * @param filePath a path to the catalog file
     */
    public CatalogResolver(String filePath) {
        resolver = new XMLCatalogResolver(new String[]{filePath});
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException,
            IOException {
        return resolver.resolveEntity(publicId, systemId);
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId,
                                   String systemId, String baseURI) {
        return resolver.resolveResource(type, namespaceURI, publicId, systemId, baseURI);
    }
}
