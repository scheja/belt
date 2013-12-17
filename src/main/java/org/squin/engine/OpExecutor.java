/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squin.dataset.jenacommon.NodeDictionary;
import org.squin.dataset.query.SolutionMapping;
import org.squin.dataset.query.TriplePattern;
import org.squin.dataset.query.arq.VarDictionary;
import org.squin.dataset.query.arq.iterators.DecodeBindingsIterator;
import org.squin.dataset.query.arq.iterators.EncodeBindingsIterator;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.GraphBase;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.main.OpExecutorFactory;

import edu.kit.aifb.belt.db.QueryGraph;
import edu.kit.aifb.belt.db.QueryGraph.QueryNode;


/**
 * A {@link com.hp.hpl.jena.sparql.engine.main.OpExecutor} implementation for
 * the {@link LinkTraversalBasedQueryEngine}.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public class OpExecutor extends org.squin.dataset.query.arq.OpExecutor
{
	static private OpExecutorFactory factory;
    static Logger l = LoggerFactory.getLogger(OpExecutor.class);

	/**
	 * Returns the factory object that creates this OpExecutor implementation.
	 */
	static public OpExecutorFactory getFactory ()
	{
		if ( factory == null ) {
			factory = new OpExecutorFactory() {
				public com.hp.hpl.jena.sparql.engine.main.OpExecutor create( ExecutionContext execCxt ) { return new OpExecutor( (LinkTraversalBasedExecutionContext) execCxt ); }
			};
		}
		return factory;
	}


	// initialization

	public OpExecutor ( LinkTraversalBasedExecutionContext execCxt )
	{
		super( execCxt );
	}


	// operations

	@Override
	public QueryIterator execute ( OpBGP opBGP, QueryIterator input )
	{
		if (    opBGP.getPattern().isEmpty()
		     || ! (execCxt.getDataset() instanceof LinkedDataCacheWrappingDatasetGraph) )
		{
			return super.execute( opBGP, input );
		}

		LinkTraversalBasedExecutionContext ltbExecCxt = (LinkTraversalBasedExecutionContext) execCxt;
		VarDictionary varDict = ltbExecCxt.varDict;
		NodeDictionary nodeDict = ltbExecCxt.nodeDict;
		
		l.info("opBGP Pattern: "+ opBGP.getPattern().toString());		
		l.info("QueryIterator: "+ input.toString());
		

		Iterator<SolutionMapping> qIt = new EncodeBindingsIterator( input, ltbExecCxt );
				
		QueryGraph queryGraph = new QueryGraph();

		for ( Triple t : opBGP.getPattern().getList() ) {
 			l.info("Created TP for Triple: <{}>", t.toString());
 			TriplePattern tp = encode(t,varDict,nodeDict);
 			QueryNode node = queryGraph.add(t,tp);
 			qIt = new NaiveTriplePatternQueryIter(tp, qIt, ltbExecCxt, node, queryGraph);
// 			qIt = new PrefetchingTriplePatternQueryIter( encode(t,varDict,nodeDict), qIt, ltbExecCxt );
//			qIt = new PostponingTriplePatternQueryIter( encode(t,varDict,nodeDict), qIt, ltbExecCxt );

		}		

		return new DecodeBindingsIterator( qIt, ltbExecCxt );
	}

}
