package com.wolterskluwer.service.content.validation.validator;

/**
 * Factory that creates validators.
 * Date: 4/9/13
 */
public class ValidatorFactory {

    private static final String PCI_PROTOCOL3
            = "http://wolterskluwer.com/services/validation/PciProtocol3Validation";

    private static final String SCHEMA
            = "http://wolterskluwer.com/services/validation/SchemaValidation";

    private static final String BUSINESS_RULE
            = "http://wolterskluwer.com/services/validation/RdfBusinessRuleValidation";

    private static final String ONTOLOGY
            = "http://wolterskluwer.com/services/validation/OntologyValidation";

    private static final String SKOS_TERM_EXISTENCE
            = "http://wolterskluwer.com/services/validation/SkosTermExistenceValidation";

    private static final String SCHEMATRON
            = "http://wolterskluwer.com/services/validation/SchematronValidation";
    
    private static final String XML_LITERALS
    	= "http://wolterskluwer.com/services/validation/XmlLiteralsValidation";

    private static final String IPACK_STRUCTURE
            = "http://wolterskluwer.com/services/validation/IPACKStructureValidation";

    private static final String DTD = "http://wolterskluwer.com/services/validation/DtdValidation";
    
    private static final String PCI_SKOS
    		= "http://wolterskluwer.com/services/validation/PciSkosRdfBusinessRuleValidation";

    private static final String CONTENT_BY_MODEL
            = "http://wolterskluwer.com/services/validation/DocumentContentByModelValidation";

	private static final String HTML5 = "http://wolterskluwer.com/services/validation/HTML5Validation";

    private final ValidationContext context;

    public ValidatorFactory() {
        context = new ValidationContext();
    }

    public Validator newValidator(String id) {
        if (PCI_PROTOCOL3.equals(id)) {
            return new PackageStructureValidator();
        } else if (SCHEMA.equals(id)) {
            return new XmlValidator();
        } else if (DTD.equals(id)) {
            return new DTDValidator();
        } else if (BUSINESS_RULE.equals(id)) {
            return new RdfBusinessRuleValidator(context);
        } else if (ONTOLOGY.equals(id)) {
            return new OntologyValidator(context);
        } else if (SKOS_TERM_EXISTENCE.equals(id)) {
            return new SkosValidator(context);
        } else if (SCHEMATRON.equals(id)) {
            return new SchematronValidator();
        } else if (XML_LITERALS.equals(id)) {
            return new XmlSnippetsValidator(context);
        } else if (IPACK_STRUCTURE.equals(id)) {
            return new IPACKStructureValidator();
        } else if (PCI_SKOS.equals(id)) {
            return new PciSkosRdfBusinessRuleValidator(context);
        } else if(CONTENT_BY_MODEL.equals(id)){
            return new DocumentContentByModelValidator();
		} else if (HTML5.equals(id)) {
			return new HTML5Validator();
		}
        throw new RuntimeException("Cannot constrcut validator for " + id);
    }
}
