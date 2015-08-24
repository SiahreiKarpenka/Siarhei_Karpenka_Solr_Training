package com.wolterskluwer.service.content.validation.validator;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.util.JenaUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.mindswap.pellet.jena.PelletReasonerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SnippetProcessor {

	private static final String QUERY_XML_LITERALS = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
			+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
			+ "PREFIX cc: <http://wolterskluwer.com/ceres/concept-v1.0/> " + "SELECT ?g ?s ?p ?o ?dt       "
			+ "WHERE {?p rdfs:range rdf:XMLLiteral ." + "?p cc:encodedDataType ?dt ." + "GRAPH ?g { ?s ?p ?o } }";

	private List<Model> ontologyModels;

	private Map<String, Model> namedRdfModels = new HashMap<String, Model>();

	public SnippetProcessor(List<Model> ontologyModels) {
		this.ontologyModels = ontologyModels;

	}

	public void addModel(String path, Model model) {
		this.namedRdfModels.put(path, model);
	}

	public String getSnippetXml() throws ValidationException {
		Model ontologyModel = ModelFactory.createDefaultModel();
		
		for (Model model : ontologyModels) {
			ontologyModel.add(model);
		}

		Reasoner reasoner = PelletReasonerFactory.theInstance().create();
		InfModel infModel = ModelFactory.createInfModel(reasoner, ontologyModel);

		Dataset dataset = DatasetFactory.create(infModel);

		for (Map.Entry<String, Model> entry : namedRdfModels.entrySet()) {
			dataset.addNamedModel(entry.getKey(), entry.getValue());
		}

		List<QuerySolution> resultList = executeSparQL(QUERY_XML_LITERALS, dataset);
		StringBuilder builder = getSnippetStringStart();
		for (QuerySolution querySolution : resultList) {
			builder.append(handleSnippet(querySolution));
		}

		builder.append("</snippet:snippet-wrapper>").append(System.getProperty("line.separator"));
		return builder.toString();
	}
	
	public void cleanModelMap() {
		namedRdfModels.clear();
	}

	private List<QuerySolution> executeSparQL(String querySt, Dataset dataset) {
		List<QuerySolution> queryResluts = new ArrayList<QuerySolution>();
		Query query = QueryFactory.create(querySt);
		QueryExecution qe = QueryExecutionFactory.create(query, dataset);
		com.hp.hpl.jena.query.ResultSet results = qe.execSelect();
		while (results.hasNext()) {
			QuerySolution solution = results.next();
			queryResluts.add(solution);
		}
		qe.close();
		return queryResluts;
	}

	private StringBuilder getSnippetStringStart() {
		return new StringBuilder().append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
				.append(System.getProperty("line.separator")).append("<snippet:snippet-wrapper ")
				.append("xsi:schemaLocation=\"http://XMLsnippet/ embedded-PCI-ltr.xsd\" ")
				.append("xmlns:snippet=\"http://XMLsnippet/\" ")
				.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
				.append("xmlns:cc=\"http:wolterskluwer.com/ceres/concept-v1.0/\" ")
				.append("xmlns:cw=\"http://wolterskluwer.com/ceres/content-warehouse-v1.0/\" ").append('>')
				.append(System.getProperty("line.separator"));
	}

	private StringBuilder handleSnippet(QuerySolution solution) {
		StringBuilder builder = new StringBuilder();
		String dataType = JenaUtil.stringize(solution.get("dt")).replace(':', '_');
		String filename = JenaUtil.stringize(solution.get("g"));
		builder.append("<snippet:property property=\"").append(JenaUtil.stringize(solution.get("p")))
				.append("\" file=\"").append(filename).append("\">").append(System.getProperty("line.separator"))
				.append("  <snippet:").append(dataType).append(" xmlns=\"http://www.w3.org/1999/xhtml\">")
				.append(StringEscapeUtils.unescapeXml(JenaUtil.stringize(solution.get("o")))).append("</snippet:").append(dataType).append('>')
				.append(System.getProperty("line.separator")).append("</snippet:property>")
				.append(System.getProperty("line.separator"));
		return builder;
	}

}
