package edu.kit.aifb.belt.learner;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squin.dataset.Triple;
import org.squin.dataset.query.SolutionMapping;
import org.squin.dataset.query.TriplePattern;
import org.squin.dataset.query.impl.FixedSizeSolutionMappingImpl;
import org.squin.engine.LinkTraversalBasedExecutionContext;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.hp.hpl.jena.graph.Node;

import edu.kit.aifb.belt.db.Action;
import edu.kit.aifb.belt.db.QValue;
import edu.kit.aifb.belt.db.QueryGraph;
import edu.kit.aifb.belt.db.State;
import edu.kit.aifb.belt.db.StateChain;
import edu.kit.aifb.belt.db.StateFactory;
import edu.kit.aifb.belt.db.dict.StringDictionary;

public class LearnPartialChainIterator {
	final private Logger log = LoggerFactory.getLogger( LearnPartialChainIterator.class );
	private List<Triple> al;
	private LinkTraversalBasedExecutionContext ltbExecCxt;
	private SolutionMapping currentInputMapping;
	private TriplePattern currentQueryPattern;
	private TriplePattern tp;
	private edu.kit.aifb.belt.db.QueryGraph.Node node;
	private QueryGraph queryGraph;

	public LearnPartialChainIterator(Iterator<? extends Triple> currentMatches, LinkTraversalBasedExecutionContext ltbExecCxt, SolutionMapping currentInputMapping, TriplePattern currentQueryPattern, TriplePattern tp, edu.kit.aifb.belt.db.QueryGraph.Node node, QueryGraph queryGraph) {
		al = new ArrayList<Triple>();
		this.ltbExecCxt = ltbExecCxt;
		this.currentInputMapping = currentInputMapping;
		this.currentQueryPattern = currentQueryPattern;
		this.tp = tp;
		this.node = node;
		this.queryGraph = queryGraph;
		int[] map = ((FixedSizeSolutionMappingImpl)currentInputMapping).getMap();
		int size = map.length;
		String solutionMappingString = "Current Solution Mapping: ";
		for ( int i = 0; i < size; ++i ) {
			if ( map[i] != SolutionMapping.UNBOUND ) {
				solutionMappingString += ltbExecCxt.varDict.getVar(i) + " (v" + String.valueOf(i) + ") -> " + ltbExecCxt.nodeDict.getNode(map[i]).toString() + " (n" + String.valueOf(map[i]) + ") :: ";
			}
		}
		
		log.info(solutionMappingString);
				
		Node currentQueryPatternS = (currentQueryPattern.sIsVar ? ltbExecCxt.varDict.getVar(currentQueryPattern.s) : ltbExecCxt.nodeDict.getNode(currentQueryPattern.s));
		Node currentQueryPatternP = (currentQueryPattern.pIsVar ? ltbExecCxt.varDict.getVar(currentQueryPattern.p) : ltbExecCxt.nodeDict.getNode(currentQueryPattern.p));
		Node currentQueryPatternO = (currentQueryPattern.oIsVar ? ltbExecCxt.varDict.getVar(currentQueryPattern.o) : ltbExecCxt.nodeDict.getNode(currentQueryPattern.o));
		log.info("QueryPattern: <{}> ({}) / <{}> ({}) / <{}> ({})", new Object[]{
				currentQueryPatternS,
				(currentQueryPattern.sIsVar?"v":"n") + String.valueOf(currentQueryPattern.s),
				currentQueryPatternP,
				(currentQueryPattern.pIsVar?"v":"n") + String.valueOf(currentQueryPattern.p),
				currentQueryPatternO,
				(currentQueryPattern.oIsVar?"v":"n") + String.valueOf(currentQueryPattern.o),
				});				
		
		while (currentMatches.hasNext()) {
			learn(currentMatches.next());			
		}
	}

	private void learn(Triple t) {
		al.add(t);
		Node s = ltbExecCxt.nodeDict.getNode(t.s);
		Node p = ltbExecCxt.nodeDict.getNode(t.p);
		Node o = ltbExecCxt.nodeDict.getNode(t.o);
		
		try {
			URL url = new URL(o.toString());
			String domain = url.getHost();
			
			Action action = new Action(domain, t.p, ((StringDictionary)ltbExecCxt.nodeDict));
			StateFactory statefactory = new StateFactory(Integer.MAX_VALUE, true);
			List<State> past1list = statefactory.createState(s.toString(), Main.getDB());
			StateChain past = new StateChain(past1list);
			State past1 = past1list.get(0);

			
 			// log.info("#QueryPattern: <{}>", tp.toString());
 			// log.info("currentQueryPattern: <{}>", currentQueryPattern.toString());
 			// log.info("Vars from the current Query Pattern:");
 			// for ( Integer i : currentQueryPattern.getVars() ) {
	 		// 	log.info("<{}>", i.toString());
			// } 	
						
 			List<TriplePattern> whatweknow = new ArrayList<TriplePattern>();
 			
 			for (int varID : currentQueryPattern.getVars()) {
 				for ( edu.kit.aifb.belt.db.QueryGraph.Node node : queryGraph.getNodes() ) {
 					TriplePattern tp1 = node.getTriplePattern();
 					if (tp1.containsVar(varID) && !tp1.equals(tp)) 
 						whatweknow.add(tp1);	
 				} 				
 			}
			
			log.info("Updated Q for:");
			log.info("History 1 for URL: {}", s.toString());
			log.info("Domain: {}, No. of Types: {}, No. of Props: {}", new Object[]{past1.getDomain(Main.getDB().getDictionary()), past1.getTypes(Main.getDB().getDictionary()).size(), past1.getProperties(Main.getDB().getDictionary()).size()});
			log.info("Action for URL: {}", o.toString());
			log.info("Domain: {}, Property: {}", action.getDomain(Main.getDB().getDictionary()), action.getProperty(Main.getDB().getDictionary()));
			log.info("Future 1 with properties:");
			Multiset<String> future1properties = HashMultiset.create();

			for ( TriplePattern tp1 : whatweknow ) {
	 			log.info("<{}>", tp1.toString());
	 			future1properties.add(ltbExecCxt.nodeDict.getNode(tp1.p).toString());
			} 	
			
			
			List<State> future1 = statefactory.createState(null, null, future1properties,  Main.getDB().getDictionary());
			StateChain future = new StateChain(future1);
			QValue q = new QValue(past, action, future);
			SimpleQLearner sql = Main.getSQL();
			sql.updateQ(q, 1, 0.5, 0.5);
			sql.updateQuality(o.toString());
			
			log.info("Match: <{}> (n{}) / <{}> (n{}) / <{}> (n{})", new Object[]{s, t.s, p, t.p, o , t.o});		
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Iterator<? extends Triple> getIterator() {
		return al.iterator();
	}
	
}
