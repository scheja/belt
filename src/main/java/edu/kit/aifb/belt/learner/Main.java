/**
 * 
 */
package edu.kit.aifb.belt.learner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squin.command.modules.ModMonitor;
import org.squin.dataset.QueriedDataset;
import org.squin.dataset.hashimpl.combined.QueriedDatasetImpl;
import org.squin.dataset.jenacommon.JenaIOBasedQueriedDataset;
import org.squin.dataset.jenacommon.NodeDictionary;
import org.squin.engine.LinkTraversalBasedQueryEngine;
import org.squin.engine.LinkedDataCacheWrappingDataset;
import org.squin.ldcache.jenaimpl.JenaIOBasedLinkedDataCache;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;

import edu.kit.aifb.belt.db.Database;
import edu.kit.aifb.belt.sourceindex.SourceIndexJenaImpl;

/**
 * @author janscheurenbrand
 * 
 */
public class Main {

    static Logger l = LoggerFactory.getLogger(Main.class);
	protected static ModMonitor modMonitor = new ModMonitor ();

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Database db = new Database("janscheurenbrand.de/belt");
		db.connect();
		db.setDictionaryFlushThreshold(100);
		
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
				+ "SELECT DISTINCT ?city \n"
				+ "WHERE {\n"
				+ "    <http://people.aifb.kit.edu/awa/foaf.rdf#andreas> foaf:knows ?friend ."
				+ "    ?friend <http://swrc.ontoware.org/ontology#affiliation> ?org ."
				+ "    ?org <http://www.w3.org/2002/07/owl#sameAs> ?dbporg ."
				+ "    ?dbporg <http://dbpedia.org/property/city> ?city"
				+ "}\n";
		
		// Step one: Get the URIs needed for this Query.
		getURIsFromQuery(queryString2, "urls.txt", db.getDictionary());
		
		// Step two: Load the triples from those URIs
		// DataRetrieverIterator.deserialize("urls.txt");
		
		// SourceIndex.test();
		
//		SourceIndex si = new SourceIndex();
//		Iterator<Quad> res = si.findAllByURI("http://people.aifb.kit.edu/awa/foaf.rdf");
//		
//		while (res.hasNext()) {
//			System.out.println(res.next());
//		}
		
		db.close();
	}


	private static void getURIsFromQuery(String queryString, String path, NodeDictionary dict) {
		LinkTraversalBasedQueryEngine.register();

		QueriedDataset qds = new QueriedDatasetImpl();
		JenaIOBasedQueriedDataset qdsWrapper = new JenaIOBasedQueriedDataset(
				qds, dict);
		JenaIOBasedLinkedDataCache ldcache = new JenaIOBasedLinkedDataCache(
				qdsWrapper);

		Dataset dsARQ = new LinkedDataCacheWrappingDataset(ldcache);
		
		modMonitor.startTimer();
		QueryExecution qe = QueryExecutionFactory.create(queryString, dsARQ);
		ResultSet results = qe.execSelect();
		System.out.println(ResultSetFormatter.asText(results));
		long time = modMonitor.endTimer();
		
		System.out.println( "Time: " + modMonitor.timeStr(time) + " sec" );
		// System.out.println( "Statistics:" );
		//ldcache.getStatistics().print( System.out, 1 );
		
		SourceIndexJenaImpl.handleRedirections();
		
		
		try {
			ldcache.shutdownNow(4000); // 4 sec.
		} catch (Exception e) {
			System.err.println("Shutting down the Linked Data cache failed: "
					+ e.getMessage());
		}
	}

}
