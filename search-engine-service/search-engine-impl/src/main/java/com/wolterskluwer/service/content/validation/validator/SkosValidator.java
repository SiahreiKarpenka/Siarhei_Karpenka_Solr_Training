package com.wolterskluwer.service.content.validation.validator;

import java.io.File;
import java.util.List;

import org.mindswap.pellet.exceptions.InconsistentOntologyException;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.wolterskluwer.service.content.validation.ConfigurationResourceAccessException;
import com.wolterskluwer.service.content.validation.Messages;
import com.wolterskluwer.service.content.validation.Params;
import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.ValidationServiceConfiguration;
import com.wolterskluwer.service.content.validation.context.OrchestrationContext;
import com.wolterskluwer.service.content.validation.reporter.Reporter;

public class SkosValidator extends AbstractRDFValidator implements Validator {

    private static final Logger LOG = LoggerFactory.getLogger(SkosValidator.class);
    private static final Messages MESSAGES = Messages.getInstance();

    static final String PARAM_SKOS_PATH = "path.skos"; // skos files
    static final String PARAM_SKOS_ADDITIONAL = "path.additional";
    static final String PARAM_EVERYHING_AS_WARNING = "everything_as_warning";
    private static final String PROPERTY_HAS_RESTRICTION = "http://wolterskluwer.com/ceres/concept-v1.0/hasRestriction";

    private boolean reportWarnings;
    
    SkosValidator(ValidationContext context) {
        super(context);
    }

    /**
     * Only for tests usage
     *
     * @param configuration
     * @param context
     */
    SkosValidator(ValidationServiceConfiguration configuration, ValidationContext context) {
        super(context);
        setConfiguration(configuration);
    }

    @Override
    public void initialize(OrchestrationContext context, Params params,
            Reporter reporter) throws ValidationException {
        super.initialize(context, params, reporter);
        reportWarnings = Boolean.valueOf(getParams().getParam(PARAM_EVERYHING_AS_WARNING));
    }

    @Override
    public void validateContent(String[] paths) throws ValidationException {
        Reporter containerReporter = super.getReporter();
        try {
            if (paths.length > 0) {
                for (String path : paths) {
                    Reporter resourceReporter = containerReporter.getResourceReporter(path);
                    setReporter(resourceReporter);
                    //validateModel(params, getRdfModel(context.getInputStream(path), path));
                    validateModel(getRdfModel(path));
                }
            } else {
                validateModel(getRdfModel("local.file"));
            }
        } catch (ConfigurationResourceAccessException ex) {
            reportError(ex.getMessage());
        } finally {
            setReporter(containerReporter);
        }
    }

    public void validateModel(Model rdfModel) throws ValidationException {
        Params params = getParams();
        String[] skosPaths = params.getParams(PARAM_SKOS_PATH);
        if (skosPaths == null) {
            throwExceptionAndLogError("msg.rdf.skos.noParam", null);
        }
        String[] additionalPaths = params.getParams(PARAM_SKOS_ADDITIONAL);
        if (additionalPaths != null && additionalPaths.length > 0) {
            completeModel(rdfModel, additionalPaths);
        }
        removeRestrictionsFromModel(rdfModel);
        Model skosModel = getSkosModel(skosPaths);
        validateAgainstSkosModel(rdfModel, skosModel);
    }

    void completeModel(Model rdfModel, String[] additionalPaths) throws ValidationException {
        List<File> additionalFiles = getConfiguration().listFilesByPattern(additionalPaths);
        for (File additionalFile : additionalFiles) {
            Model additionalModel = getModelFromCache(additionalFile);
            rdfModel.add(additionalModel);
        }
    }

    Model removeRestrictionsFromModel(Model model) {
        model.removeAll(null, model.createProperty(PROPERTY_HAS_RESTRICTION), (RDFNode) null);
        return model;
    }

    Model getSkosModel(String[] skosPaths) throws ValidationException {
        List<File> skosFiles = getConfiguration().listFilesByPattern(skosPaths);
        Model model = ModelFactory.createDefaultModel();
        for (File skosFile : skosFiles) {
            Model skosModel = getModelFromCache(skosFile);
            model.add(skosModel);
        }
        return model;
    }

    private void validateAgainstSkosModel(Model model, Model skosModel) {
        try {
            Reasoner reasoner = createPelletReasoner();
            InfModel infModel = ModelFactory.createInfModel(reasoner, model);
            Property propertySkosInScheme = infModel.createProperty("http://www.w3.org/2004/02/skos/core#inScheme");
            StmtIterator statementIterator = infModel.listStatements(null, propertySkosInScheme, (RDFNode) null);
            while (statementIterator.hasNext()) {
                Statement statement = statementIterator.next();
                if (!skosModel.containsResource(statement.getSubject())) {
                    reportError(composeErrorMessage(statement.getSubject().toString()));
                }
            }
        } catch (InconsistentOntologyException e) {
            reportError(MESSAGES.getMessage("msg.rdf.invalid.skos")
                    + e.getMessage().replace("\n", " ").replace("\r", ""));
            LOG.error("Error during SKOS validation: ", e);
        }
    }

    private String composeErrorMessage(String invalidTerm) {
        StringBuilder sb = new StringBuilder();
        sb.append(MESSAGES.getMessage("msg.rdf.invalid.skos"));
        sb.append(MESSAGES.getMessage("msg.rdf.skos.error"));
        sb.append(invalidTerm);
        return sb.toString();
    }

    private static Reasoner createPelletReasoner() {
        return PelletReasonerFactory.theInstance().create();
    }

    @Override
    protected Reporter getReporter() {
        throw new UnsupportedOperationException(
                "Use reportError or getResourceReporter method instead of calling this." +
                "This is made to prevent direct reporting without cheking validator parameters");
    }

    protected void reportError(String message) {
        Reporter reporter = super.getReporter();
        if (reportWarnings) {
            reporter.warn(message);
        } else {
            reporter.error(message);
        }
    }
}
