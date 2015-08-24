package com.wolterskluwer.service.content.validation.util;

import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * User: Karsten.Engelke
 * Date: 2015-03-26
 * Time: 10:27
 */
public class JenaUtil {
    public static String stringize(RDFNode node) {
        if (node.isLiteral()) {
            return node.asLiteral().getLexicalForm();
        } else if (node.isAnon()) {
            return "";
        } else if (node.isResource()) {
            return node.asResource().getURI();
        } else {
            return null;
        }
    }
}
