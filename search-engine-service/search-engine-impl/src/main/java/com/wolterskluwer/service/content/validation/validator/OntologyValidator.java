package com.wolterskluwer.service.content.validation.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import com.wolterskluwer.service.content.validation.ConfigurationResourceAccessException;
import com.wolterskluwer.service.content.validation.Params;
import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.ValidationServiceConfiguration;
import com.wolterskluwer.service.content.validation.reporter.Reporter;

public class OntologyValidator extends AbstractRDFValidator implements Validator {

    static final String PARAM_ONTOLOGY_PATH = "path.ontology"; // ontology files
    static final String PARAM_CLOSED_WORLD = "closed-world";

    OntologyValidator(ValidationContext context) {
        super(context);
    }

    /**
     * Only for tests usage
     * 
     * @param configuration
     * @param context
     */
    OntologyValidator(ValidationServiceConfiguration configuration, ValidationContext context) {
        super(context);
        setConfiguration(configuration);
    }

    @Override
    protected void validateContent(String[] paths) throws ValidationException {
        try {
            if (paths.length > 0) {
                if (shouldAggregateModels()) {
                    validateModelsAggregated(paths);
                } else {
                    validateModelsOneByOne(paths);
                }
            } else {
                validateModel(getParams(), getRdfModel("local.file"));
            }
        } catch (ConfigurationResourceAccessException ex) {
            getReporter().error(ex.getMessage());
        }
    }

    private boolean shouldAggregateModels() {
        return Boolean.parseBoolean(getParams().getParam(PARAM_AGGREGATE));
    }

    private void validateModelsAggregated(String[] paths) throws ValidationException {
        setReporter(getReporter().getResourceReporter("rdf:merged-model"));
        Model aggregatedModel = ModelFactory.createDefaultModel();
        for (String path : paths) {
            aggregatedModel.add(getRdfModel(path));
        }
        validateModel(getParams(), aggregatedModel);
    }

    private void validateModelsOneByOne(String[] paths) throws ValidationException {
        Reporter containerReporter = super.getReporter();
        for (String path : paths) {
            setReporter(containerReporter.getResourceReporter(path));
            validateModel(getParams(), getRdfModel(path));
        }
        setReporter(containerReporter);
    }

    void validateModel(Params params, Model rdfModel) throws ValidationException {
        String[] ontologyPaths = params.getParams(PARAM_ONTOLOGY_PATH);

        if (ontologyPaths == null) {
            throwExceptionAndLogError("msg.rdf.ontology.noParam", null);
        }

        List<Model> ontologyModels = getModelsFromPaths(ontologyPaths);
        List<String> closedWorldParams = new ArrayList<String>();
        if (params.getParams(PARAM_CLOSED_WORLD) != null) {
            closedWorldParams.addAll(Arrays.asList(params.getParams(PARAM_CLOSED_WORLD)));
        }
        if (closedWorldParams.contains("true")) {
            validateClassesAndProperties(rdfModel, ontologyModels, getReporter());
        }
        if(isValidReasonerName(params.getParam(PARAM_REASONER))) {
            validateModelAgainstOntologies(createReasoner(params.getParam(PARAM_REASONER)), rdfModel, ontologyModels, getReporter());
        } else {
            throwExceptionAndLogError("msg.rdf.ontology.unknownReasoner", null, params.getParam(PARAM_REASONER));
        }
    }


    private void validateClassesAndProperties(Model model, List<Model> ontologyModels,
            Reporter reporter) throws ValidationException {
        List<String> allowedClassesAndProperties = getAllowedClassesAndProperties(aggregateModels(ontologyModels));
        for (Statement statement : model.listStatements().toList()) {
            String predicateUri = statement.getPredicate().getURI().toString();
            String objectUri = statement.getObject().isURIResource() ? statement.getObject()
                    .asResource().getURI().toString() : null;
            if (objectUri != null && statement.getPredicate() == RDF.type
                    && !allowedClassesAndProperties.contains(objectUri)) {
                StringBuilder sb = new StringBuilder("Invalid class: ");
                sb.append(objectUri);
                sb.append(" is not mentioned in the ontology");
                reporter.error(sb.toString());
            }
            if (predicateUri != null && !allowedClassesAndProperties.contains(predicateUri)) {
                StringBuilder sb = new StringBuilder("Invalid predicate: ");
                sb.append(predicateUri);
                sb.append(" is not mentioned in the ontology");
                reporter.error(sb.toString());
            }
        }
    }

    private List<String> getAllowedClassesAndProperties(Model ontModel) {
        Query query = QueryFactory.create("select distinct ?class where "
                + "{ { ?class a <http://www.w3.org/2002/07/owl#Class> } "
                + "union { ?class a <http://www.w3.org/2002/07/owl#ObjectProperty> } "
                + "union { ?class a <http://www.w3.org/2002/07/owl#OntologyProperty> } "
                + "union { ?class a <http://www.w3.org/2002/07/owl#DatatypeProperty> } "
                + "union { ?class a <http://www.w3.org/2002/07/owl#DeprecatedProperty> } "
                + "union { ?class a <http://www.w3.org/2002/07/owl#FunctionalProperty> } "
                + "union { ?class a <http://www.w3.org/2002/07/owl#InverseFunctionalProperty> } "
                + "union { ?class a <http://www.w3.org/2002/07/owl#ReflexiveProperty> } "
                + "union { ?class a <http://www.w3.org/2002/07/owl#IrreflexiveProperty> } "
                + "union { ?class a <http://www.w3.org/2002/07/owl#AnnotationProperty> } "
                + "union { ?class a <http://www.w3.org/2002/07/owl#SymmetricProperty> } "
                + "union { ?class a <http://www.w3.org/2002/07/owl#AsymmetricProperty> } "
                + "union { ?class a <http://www.w3.org/2002/07/owl#TransitiveProperty> } "
                + "union { ?class a <http://www.w3.org/1999/02/22-rdf-syntax-ns#Literal> } "
                + "filter (!isBlank(?class)) }");
        query.setSyntax(Syntax.syntaxARQ);
        QueryExecution queryExecution = QueryExecutionFactory.create(query, ontModel);
        ResultSet resultSet = queryExecution.execSelect();
        Set<String> allowed = new HashSet<String>();
        for (; resultSet.hasNext();) {
            QuerySolution querySolution = resultSet.nextSolution();
            allowed.add(querySolution.get("?class").toString());
        }
        // always allowed, but not in the ontology
        allowed.add(RDF.type.getURI());
        return new ArrayList<String>(allowed);
    }
}
