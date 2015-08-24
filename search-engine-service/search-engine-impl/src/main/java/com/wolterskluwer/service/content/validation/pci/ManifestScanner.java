package com.wolterskluwer.service.content.validation.pci;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.wolterskluwer.services.library.xml.Elements;
import com.wolterskluwer.services.library.xml.beans.ElementPattern;

/**
 * Scans a PCI manifest document for file paths.
 * Note: This class is very specific to PCI manifest structure, and should not be used in the future!
 */
public class ManifestScanner {

    private static final Logger log = LoggerFactory.getLogger(ManifestScanner.class);
    public static final String OBJECT_MCP_PATH_ATTR = "cip:object-MCP-path-attr";
    private ArrayList<ManifestElementFilter> elementFilters = new ArrayList<ManifestElementFilter>();
    private String[] paths;
    private Document manifestDocument;

    public ManifestScanner(Document manifestDocument, List<ManifestElementFilter> filters) {
        this.manifestDocument = manifestDocument;
        elementFilters.addAll(filters);
    }

    private static String[] listAttributeValues(Element[] elements, String attributeName) {
        if (elements == null) {
            throw new IllegalArgumentException("The elements argument is null");
        }
        int length = elements.length;
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            values[i] = elements[i].getAttribute(attributeName);
        }
        return values;
    }

    public void scan() throws SAXException, IOException, ParserConfigurationException {
        log.debug("Scanning manifest file for resources...");
        ArrayList<Element> foundElements = new ArrayList<Element>();
        for (ManifestElementFilter elementFilter : elementFilters) {
            Element[] elements = Elements.getElements(manifestDocument, createPattern(elementFilter));
            Collections.addAll(foundElements, elements);
        }
        paths = listAttributeValues(
                foundElements.toArray(new Element[foundElements.size()]),
                OBJECT_MCP_PATH_ATTR);
        log.info("Found {} file resources", paths.length);
    }

    public String[] getPaths() {
        if (paths == null) {
            String message = "Must call scan() first";
            log.error(message);
            throw new IllegalStateException(message);
        }
        return Arrays.copyOf(paths, paths.length);
    }

    public String[] lookupPaths(String type, String aspect) {
        ManifestElementFilter filter = createAddReplaceFilter(type, aspect);
        ElementPattern pattern = createPattern(filter);
        Element[] elements = Elements.getElements(manifestDocument, pattern);
        return listAttributeValues(elements, OBJECT_MCP_PATH_ATTR);
    }

    private ManifestElementFilter createAddReplaceFilter(String type, String aspect) {
        ManifestElementFilter filter = new ManifestElementFilter();
        filter.setAspect(aspect);
        filter.setType(type);
        return filter;
    }

    private ElementPattern createPattern(ManifestElementFilter elementFilter) {
        ElementPattern elementPattern = new ElementPattern("add-replace");
        elementPattern.useNamespace(true);
        String aspect = elementFilter.getAspect();
        if (aspect != null) {
            elementPattern.setAttribute("cip:aspect", aspect);
        }
        String type = elementFilter.getType();
        if (type != null) {
            elementPattern.setAttribute("cip:object-type-attr", type);
        }
        return elementPattern;
    }
}
