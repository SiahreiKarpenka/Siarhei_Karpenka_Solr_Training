package com.wolterskluwer.service.content.validation.util;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum ReasonerName {

    BASIC_FORWARD_RULE_REASONER("BasicForwardRuleReasoner"),
    FBRULE_REASONER("FBRuleReasoner"),
    GENERIC_RULE_REASONER("GenericRuleReasoner"),
    LPBACKWARD_RULE_REASONER("LPBackwardRuleReasoner"),
    OWL_FBRULE_REASONER("OWLFBRuleReasoner"),
    OWL_MICRO_REASONER("OWLMicroReasoner"),
    OWL_MINI_REASONER("OWLMiniReasoner"),
    RDFS_FBRULE_REASONER("RDFSFBRuleReasoner"),
    RDFS_FORWARD_RULE_REASONER("RDFSForwardRuleReasoner"),
    RDFS_RULE_REASONER("RDFSRuleReasoner"),
    TRANSITIVE_REASONER("TransitiveReasoner"),
    PELLET("Pellet");

    private static final Map<String, ReasonerName> nameToValueMap = new HashMap<String, ReasonerName>();

    static {
        for (ReasonerName value : EnumSet.allOf(ReasonerName.class)) {
            nameToValueMap.put(value.getReasonerName(), value);
        }
    }

    private final String name;

    ReasonerName(String name) {
        this.name = name;
    }

    public String getReasonerName() {
        return name;
    }

    public static ReasonerName forName(String name) {
        return nameToValueMap.get(name);
    }
}
