package edu.kit.aifb.belt.searcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squin.command.modules.ModMonitor;
import org.squin.dataset.QueriedDataset;
import org.squin.dataset.hashimpl.combined.QueriedDatasetImpl;
import org.squin.dataset.jenacommon.JenaIOBasedQueriedDataset;
import org.squin.engine.LinkTraversalBasedQueryEngine;
import org.squin.engine.LinkedDataCacheWrappingDataset;
import org.squin.ldcache.jenaimpl.JenaIOBasedLinkedDataCache;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;

import edu.kit.aifb.belt.db.Database;
import edu.kit.aifb.belt.learner.SimpleQLearner;

/**
 * @author janscheurenbrand
 * 
 */
public class Main {

    static Logger l = LoggerFactory.getLogger(Main.class);
	protected static ModMonitor modMonitor = new ModMonitor ();
	private static Database db = null;
	private static SimpleQLearner sql = null;
	protected static boolean isLearning = false;

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		isLearning = false;
		execute();			
	}

	protected static void execute() {
		db = new Database();
		db.connect();
		db.setDictionaryFlushThreshold(100);

		sql = new SimpleQLearner(Long.MAX_VALUE, db, null);
		sql.start();
		
		String query = new QueryFactory().getQuery("andisFriendsDBUnis");
		
		LinkTraversalBasedQueryEngine.register();
		QueriedDataset qds = new QueriedDatasetImpl();
		JenaIOBasedQueriedDataset qdsWrapper = new JenaIOBasedQueriedDataset(qds, db.getDictionary());
		JenaIOBasedLinkedDataCache ldcache = new JenaIOBasedLinkedDataCache(qdsWrapper);
		Dataset dsARQ = new LinkedDataCacheWrappingDataset(ldcache);
		
		modMonitor.startTimer();
		QueryExecution qe = QueryExecutionFactory.create(query, dsARQ);		
		ResultSet results = qe.execSelect();	
		System.out.println(ResultSetFormatter.asText(results));
		long time = modMonitor.endTimer();
		
		System.out.println( "Time: " + modMonitor.timeStr(time) + " sec" );		
		
		db.handleRedirections();
		sql.stop();
		db.close();
		
		try {
			ldcache.shutdownNow(4000);
		} catch (Exception e) {
		}
	}

	public static Database getDB() {
		return db;
	}
	
	public static SimpleQLearner getSQL() {
		return sql;
	}	
	
	public static boolean isLearning() {
		return isLearning;
	}

}
