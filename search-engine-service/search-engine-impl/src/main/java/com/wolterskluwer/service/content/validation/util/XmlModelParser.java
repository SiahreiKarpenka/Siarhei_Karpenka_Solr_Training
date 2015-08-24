package com.wolterskluwer.service.content.validation.util;

import com.hp.hpl.jena.rdf.model.Model;
import java.io.InputStream;


/**
 * Reads model from an InputStream. The given InputStream should contain data in RDF/XML language.
 *
 */
public interface XmlModelParser {
    Model parseModel(InputStream in);
}
