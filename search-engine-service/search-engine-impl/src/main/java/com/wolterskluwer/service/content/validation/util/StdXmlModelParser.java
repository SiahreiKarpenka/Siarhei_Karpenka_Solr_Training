package com.wolterskluwer.service.content.validation.util;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * Date: 7/22/13
 */
public class StdXmlModelParser implements XmlModelParser {

    private StdXmlModelParser() {
    }

    private static class InstanceHolder {
        public static final StdXmlModelParser INSTANCE = new StdXmlModelParser();
    }

    public static StdXmlModelParser getInstance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public Model parseModel(InputStream in) {
        try {
            Model model = ModelFactory.createDefaultModel();
            return model.read(in, null);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
