/**
 * 
 */
package edu.kit.aifb.belt.learner;

import java.util.ArrayList;
import java.util.List;

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
import com.hp.hpl.jena.sparql.core.TriplePath;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;

import edu.kit.aifb.belt.db.Database;

/**
 * @author janscheurenbrand
 * 
 */
public class Main {

    static Logger l = LoggerFactory.getLogger(Main.class);
	protected static ModMonitor modMonitor = new ModMonitor ();
	private static Database db = null;
	private static SimpleQLearner sql = null;

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		db = new Database();
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
		
		sql = new SimpleQLearner(Long.MAX_VALUE, db, null);
		sql.start();		
		
		learnQuery(queryString2, db.getDictionary());		
		sql.stop();
		db.close();
	}
	
	public static Database getDB() {
		return db;
	}
	
	public static SimpleQLearner getSQL() {
		return sql;
	}	


	private static void learnQuery(String queryString,NodeDictionary dict) {
		LinkTraversalBasedQueryEngine.register();
		QueriedDataset qds = new QueriedDatasetImpl();
		JenaIOBasedQueriedDataset qdsWrapper = new JenaIOBasedQueriedDataset(qds, dict);
		JenaIOBasedLinkedDataCache ldcache = new JenaIOBasedLinkedDataCache(qdsWrapper);
		Dataset dsARQ = new LinkedDataCacheWrappingDataset(ldcache);
		
		modMonitor.startTimer();
		QueryExecution qe = QueryExecutionFactory.create(queryString, dsARQ);		
		ResultSet results = qe.execSelect();
		
		l.info("List of all Triples:");
		
		ElementGroup eg = (ElementGroup) qe.getQuery().getQueryPattern();
		List<ElementPathBlock> epbl = new ArrayList<ElementPathBlock>();
		for (Element e : eg.getElements()) {
			epbl.add((ElementPathBlock) e);
		}
		
		for (ElementPathBlock epb : epbl) {
			List<TriplePath> tpl = epb.getPattern().getList();
			
			for (TriplePath tp : tpl) {
				l.info(tp.toString());
			}
		}
		
	
		System.out.println(ResultSetFormatter.asText(results));
		long time = modMonitor.endTimer();
		
		System.out.println( "Time: " + modMonitor.timeStr(time) + " sec" );		
		
		db.handleRedirections();
		
		try {
			ldcache.shutdownNow(4000);
		} catch (Exception e) {
			System.err.println("Shutting down the Linked Data cache failed: " + e.getMessage());
		}
	}

}
