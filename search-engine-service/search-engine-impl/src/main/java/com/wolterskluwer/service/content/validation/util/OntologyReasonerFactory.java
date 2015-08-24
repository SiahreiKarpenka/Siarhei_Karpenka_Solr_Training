package com.wolterskluwer.service.content.validation.util;

import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.*;
import com.hp.hpl.jena.reasoner.transitiveReasoner.TransitiveReasonerFactory;
import org.mindswap.pellet.jena.PelletReasonerFactory;

/**
 * Created by Siarhei_Karpenka on 4/16/2015.
 */
public class OntologyReasonerFactory {

    public static Reasoner createReasoner(ReasonerName reasonerName) {
        switch (reasonerName) {
            case GENERIC_RULE_REASONER: {
                return GenericRuleReasonerFactory.theInstance().create(null);
            }
            case OWL_FBRULE_REASONER: {
                return OWLFBRuleReasonerFactory.theInstance().create(null);
            }
            case OWL_MICRO_REASONER: {
                return OWLMicroReasonerFactory.theInstance().create(null);
            }
            case OWL_MINI_REASONER: {
                return OWLMiniReasonerFactory.theInstance().create(null);
            }
            case RDFS_FBRULE_REASONER: {
                return RDFSFBRuleReasonerFactory.theInstance().create(null);
            }
            case RDFS_RULE_REASONER: {
                return RDFSRuleReasonerFactory.theInstance().create(null);
            }
            case TRANSITIVE_REASONER: {
                return TransitiveReasonerFactory.theInstance().create(null);
            }
            case PELLET: {
                return PelletReasonerFactory.theInstance().create();
            }
            case BASIC_FORWARD_RULE_REASONER: {
                return null;
            }
            case FBRULE_REASONER: {
                return null;
            }
            case LPBACKWARD_RULE_REASONER: {
                return null;
            }
            case RDFS_FORWARD_RULE_REASONER: {
                return null;
            }
            default: {
                return OWLMiniReasonerFactory.theInstance().create(null);
            }
        }
    }

    public static Reasoner createReasoner() {
        return OWLMiniReasonerFactory.theInstance().create(null);
    }
}
