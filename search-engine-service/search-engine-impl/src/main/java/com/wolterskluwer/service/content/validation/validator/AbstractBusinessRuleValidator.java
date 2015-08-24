package com.wolterskluwer.service.content.validation.validator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.wolterskluwer.service.content.validation.ConfigurationResourceAccessException;
import com.wolterskluwer.service.content.validation.Params;
import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.context.OrchestrationContext;
import com.wolterskluwer.service.content.validation.reporter.Reporter;

public abstract class AbstractBusinessRuleValidator extends AbstractRDFValidator {

    /**
     * The name of the parameter that specifies a glob pattern to look for SPARQL files in the
     * configuration pack, in order to validate the package SKOS RDF files against.
     */
    protected static final String PARAM_PATH_RULES = "path.rules";
    
    /**
     * Reference to the <code>Params</code> instance passed in the validator. The params object
     * reflects the parameters specified in the orchestration.xml
     */
    //protected Params params;

    /**
     * Reference to the <code>OrchestrationContext</code> passed in the validator which provedes
     * access to the configuration resources as well as the input package.
     */
    //protected OrchestrationContext orchestrationContext;
    
    /**
     * Initializes the state of the validator. This method should be invoked first in the validate()
     * entry point.
     *
     * @param context   an <code>OrchestrationContext</code> instance passed in the validator
     * @param params    a <code>Params</code> instance passed in the validator
     * @param reporter  a <code>Reporter</code> instance passed in the validator
     */
    protected void init(OrchestrationContext context, Params params, Reporter reporter)
            throws ValidationException {
        initialize(context, params, reporter);
    }
    
	AbstractBusinessRuleValidator(ValidationContext context) {
		super(context);
	}
	
    /**
     * Retrieves SPARQL files from the configuration pack. The glob pattern which is responsible for
     * filtering SPARQL files that are used for validation is configurable through the
     * orchestration.xml. The later must provide mandatory parameter {@link #PARAM_PATH_RULES}.
     * In case {@link #PARAM_PATH_RULES} parameter is missed, the validator exits with an error.
     *
     * @return a list of SPARQL files that will be used to validate the input package
     * @throws ConfigurationResourceAccessException if a configuration resource is not accessible
     * @throws ValidationException if the {@link #PARAM_PATH_RULES} parameter is missed
     */
    List<String> getQueriesFromProperties()
            throws ParserConfigurationException, SAXException, IOException,
            ValidationException {
        String[] wildcards = getParamValues(PARAM_PATH_RULES);
        ArrayList<String> list = new ArrayList<String>();
        List<File> files = getConfiguration().listFilesByPattern(wildcards);
        for (File file : files) {
            list.addAll(SparqlUtil.extractQueriesFromFile(file));
        }
        return list;
    }
    
    String[] getParamValues(String name)
            throws ValidationException {
        String[] values = getParams().getParams(name);
        if (values == null || values.length == 0) {
            throw new ValidationException("Param is undefined: " + name);
        }
        return values;
    }
    
    void validateModel(Model model, List<String> queries) {
        for (String queryText : queries) {
            Query query = SparqlUtil.compileQuery(queryText);
            validateModelWithSparqlRule(model, query);
        }
    }

    void validateModelWithSparqlRule(Model model, Query query) {
        QueryExecution execution = QueryExecutionFactory.create(query, model);
        if (query.isConstructType()) {
            Model resultModel = execution.execConstruct();
            validateResultModel(resultModel);
        }
    }
    
    void validateResultModel(Model model) {
        if (model.isEmpty()) {
            return;
        }
        StmtIterator it = model.listStatements();
        while (it.hasNext()) {
            Statement statement = it.next();
            RDFNode object = statement.getObject();
            if (object != null && object.isLiteral()) {
            	addValidationError(object.asLiteral().toString());
            }
        }
    }
    
    abstract void addValidationError(String jenaError);

}
