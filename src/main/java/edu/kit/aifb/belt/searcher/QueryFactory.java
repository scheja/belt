package edu.kit.aifb.belt.searcher;

import java.util.HashMap;
import java.util.HashSet;

public class QueryFactory {
	
	private HashMap<String, String> queries;
	
	public QueryFactory() {
		queries = new HashMap<String,String>();
		String queryString1 = "PREFIX swc: <http://data.semanticweb.org/ns/swc/ontology#>\n"
				+ "PREFIX swrc: <http://swrc.ontoware.org/ontology#>\n"
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
				+ "PREFIX foaf:   <http://xmlns.com/foaf/0.1/>\n"
				+ "SELECT DISTINCT ?author ?phone\n"
				+ "WHERE {\n"
				+ "    <http://data.semanticweb.org/conference/eswc/2009/proceedings> swc:hasPart ?pub .\n"
				+ "    ?pub swrc:author ?author .\n"
				+ "    { ?author owl:sameAs ?authAlt } UNION { ?authAlth owl:sameAs ?author }\n"
				+ "    ?authAlt foaf:phone ?phone .\n" + "}\n";
		
		String queryString2 = "\n"
				+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
				+ "SELECT ?city \n"
				+ "WHERE {\n"
				+ "    <http://people.aifb.kit.edu/awa/foaf.rdf#andreas> foaf:knows ?friend ."
				+ "    ?friend <http://swrc.ontoware.org/ontology#affiliation> ?org ."
				+ "    ?org <http://www.w3.org/2002/07/owl#sameAs> ?dbporg ."
				+ "    ?dbporg <http://dbpedia.org/property/city> ?city"
				+ "}\n";
		queries.put("phonesOfAuthors",queryString1);
		queries.put("andisFriendsDBUnis",queryString2);
	}
	
	public String getQuery(String key) {
		return queries.get(key);		
	}

}
