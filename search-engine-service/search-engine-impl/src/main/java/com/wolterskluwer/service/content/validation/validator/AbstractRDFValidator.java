package com.wolterskluwer.service.content.validation.validator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.reasoner.ValidityReport.Report;
import com.wolterskluwer.service.content.validation.ConfigurationResourceAccessException;
import com.wolterskluwer.service.content.validation.Messages;
import com.wolterskluwer.service.content.validation.Params;
import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.ValidationServiceConfiguration;
import com.wolterskluwer.service.content.validation.context.OrchestrationContext;
import com.wolterskluwer.service.content.validation.reporter.Reporter;
import com.wolterskluwer.service.content.validation.util.CatalogXmlModelParser;
import com.wolterskluwer.service.content.validation.util.OntologyReasonerFactory;
import com.wolterskluwer.service.content.validation.util.ReasonerName;
import com.wolterskluwer.service.content.validation.util.StdXmlModelParser;
import com.wolterskluwer.service.content.validation.util.XmlModelParser;

public abstract class AbstractRDFValidator implements Validator {
    
    public static final String PARAM_REASONER = "reasoner";
    
    // TODO add support for aggregate functionality
    public static final String PARAM_AGGREGATE = "additional.model";
    
    public static final String PARAM_ADDITIONAL_MODEL = "additional.model";

    private ValidationContext validationContext;

    private ValidationServiceConfiguration configuration;

    private XmlModelParser modelParser;

    private OrchestrationContext orchestrationContext;

    private Params params;

    private Reporter reporter;

    AbstractRDFValidator(ValidationContext validationContext) {
        this.validationContext = validationContext;
    }

	Model getModelFromCache(File file) throws ValidationException {
		String keyName = null;
		try {
			keyName = file.getCanonicalPath();
		} catch (IOException e) {
			throwExceptionAndLogError("msg.rdf.fileNotFound", e, file.getAbsolutePath());
		}
		return configuration.getCacheModel(keyName);
	}

	/**
	 * Retrieve model from cache, or from inputStream
	 * 
	 * @param inputStream - input data to returne model
	 * @param path - uses as a key to store kached model
	 * @return
	 * @throws ValidationException 
	 */
    protected Model getRdfModel(InputStream inputStream, String path) throws ValidationException {
        return getContext().getModel(inputStream, path);
    }

	protected void throwExceptionAndLogError(String messagePropertiesName, Exception ex, Object... params)
			throws ValidationException {
		String error = String.format(Messages.getInstance().getMessage(messagePropertiesName), params);
		reporter.error(error);
		throw new ValidationException(error, ex);
	}
	
	public ValidationContext getContext() {
		return validationContext;
	}
	
	List<Model> getModelsFromPaths(String[] ontologyPaths)
			throws ValidationException {
		List<Model> ontologyModels = new ArrayList<Model>();
		List<File> ontologyFiles = getConfiguration().listFilesByPattern(ontologyPaths);
		for (File ontologyFile : ontologyFiles) {
			Model ontologyModel = getModelFromCache(ontologyFile);
			ontologyModels.add(ontologyModel);
		}
		return ontologyModels;
	}

    @Override
	public void validate(OrchestrationContext context, Params params,
			Reporter reporter, String... paths) throws ValidationException {
		initialize(context, params, reporter);
		try {
			validateContent(paths);
		} catch (IOException e) {
			throw new ValidationException(e.getMessage(), e);
		}
	}

    public void initialize(OrchestrationContext context, Params params, Reporter reporter) throws ValidationException {
        this.orchestrationContext = context;
        this.params = params;
        this.reporter = reporter;
        configureModelParser();
    }

    private void configureModelParser() throws ConfigurationResourceAccessException {
        String catalogPath = getParams().getParam("path.catalog");
        if (isDefined(catalogPath)) {
            File catalog = getConfiguration().getFileResource(catalogPath);
            modelParser = CatalogXmlModelParser.withCatalog(catalog);
        } else {
            modelParser = StdXmlModelParser.getInstance();
        }
    }

    abstract protected void validateContent(String[] paths) throws ValidationException, IOException;

    protected Reporter getReporter() {
        return reporter;
    }

    protected void setReporter(Reporter reporter) {
        this.reporter = reporter;
    }

    protected OrchestrationContext getOrchestrationContext() {
        return orchestrationContext;
    }

    protected Params getParams() {
        return params;
    }

    protected ValidationServiceConfiguration getConfiguration() {
        if (configuration == null) {
            configuration = getOrchestrationContext().getConfiguration();
        }
        return configuration;
    }

    protected void setConfiguration(ValidationServiceConfiguration configuration) {
        this.configuration = configuration;
    }

    protected Model getRdfModel(String path) throws ValidationException {
        InputStream in = getOrchestrationContext().getInputStream(path);
        return modelParser.parseModel(in);
    }
    
    protected Model aggregateModels(List<Model> modelList) {
        Model result = ModelFactory.createDefaultModel();
        for (Model model : modelList) {
            result.add(model);
        }
        return result;
    }
    
    protected boolean isValidReasonerName(String reasonerName) {
        return ReasonerName.forName(reasonerName) != null || reasonerName == null;
    }

    protected Reasoner createReasoner(String reasonerName) {
        Reasoner reasoner = null;
        if (reasonerName == null) {
            reasoner = OntologyReasonerFactory.createReasoner();
        } else {
            reasoner = OntologyReasonerFactory.createReasoner(ReasonerName.forName(reasonerName));
        }
        return reasoner;
    }

    protected void validateModelAgainstOntologies(Reasoner reasoner, Model rdfModel, List<Model> ontologyModels,
            Reporter reporter) {
        for (Model ontologyModel : ontologyModels) {
            rdfModel.add(ontologyModel);
        }
        InfModel infModel = ModelFactory.createInfModel(reasoner, rdfModel);
        ValidityReport report = infModel.validate();
        if (!report.isValid()) { // if the report is invalid
            Iterator<Report> it = report.getReports();
            while (it.hasNext()) {
                Report errorReport = it.next();
                String messageText = Messages.getInstance().getMessage("msg.rdf.invalid.ontology");
                StringBuilder errorMessage = new StringBuilder(messageText);
                errorMessage.append(errorReport.type);
                errorMessage.append(' ');
                errorMessage.append(errorReport.description);
                reporter.error(errorMessage.toString());
            }
        }
    }

    /**
     * Returns <code>false</code> if the argument is null, empty string, or consists of only
     * whitespace characters; otherwise returns <code>true</code>
     * @param value the value to test
     * @return
     */
    private static boolean isDefined(String value) {
        return value != null && !"".equals(value.trim());
    }
}
