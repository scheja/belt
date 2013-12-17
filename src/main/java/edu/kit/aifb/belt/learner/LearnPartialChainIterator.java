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
import edu.kit.aifb.belt.db.QueryGraph.QueryNode;
import edu.kit.aifb.belt.db.State;
import edu.kit.aifb.belt.db.StateChain;
import edu.kit.aifb.belt.db.StateFactory;
import edu.kit.aifb.belt.db.dict.StringDictionary;

public class LearnPartialChainIterator {
	final private Logger log = LoggerFactory.getLogger( LearnPartialChainIterator.class );
	private List<Triple> al;
	private LinkTraversalBasedExecutionContext ltbExecCxt;
	private QueryNode node;

	public LearnPartialChainIterator(Iterator<? extends Triple> currentMatches, LinkTraversalBasedExecutionContext ltbExecCxt, SolutionMapping currentInputMapping, TriplePattern currentQueryPattern, TriplePattern tp, QueryNode node, QueryGraph queryGraph) {
		al = new ArrayList<Triple>();
		this.ltbExecCxt = ltbExecCxt;
		this.node = node;
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
			Triple t = currentMatches.next();
			node.addBinding(t);
			learn(t,0);	
			al.add(t);
		}
	}

	private void learn(Triple t, int depth) {
		Node s = ltbExecCxt.nodeDict.getNode(t.s);
		Node p = ltbExecCxt.nodeDict.getNode(t.p);
		Node o = ltbExecCxt.nodeDict.getNode(t.o);
		
		log.info("Learning triple: {} {} {} ({})", new Object[]{s,p,o,t});
		log.info("Depth: {}", depth);
		
		try {
			URL url = new URL(o.toString());
			String domain = url.getHost();
			
			Action action = new Action(domain, t.p, ((StringDictionary)ltbExecCxt.nodeDict));
			StateFactory statefactory = new StateFactory(Integer.MAX_VALUE, true);
			List<State> history1list = statefactory.createState(s.toString(), Main.getDB());
			StateChain history = new StateChain(history1list);
			State past1 = history1list.get(0);

			Multiset<String> future1properties = HashMultiset.create();	
			for ( edu.kit.aifb.belt.db.QueryGraph.Edge edge : node.outEdges) {				
				future1properties.add(edge.to.getJenaTriple().getPredicate().toString());
			}			
			
			List<State> future1 = statefactory.createState(null, null, future1properties,  Main.getDB().getDictionary());
			StateChain future = new StateChain(future1);
			QValue q = new QValue(history, action, future);
			SimpleQLearner sql = Main.getSQL();
			sql.updateQ(url.toString(), q, 0.5, 0.5, true);

			log.info("History 1 for URL: {}", s.toString());
			log.info("Domain: {}, No. of Types: {}, No. of Props: {}", new Object[]{past1.getDomain(Main.getDB().getDictionary()), past1.getTypes(Main.getDB().getDictionary()).size(), past1.getProperties(Main.getDB().getDictionary()).size()});
			log.info("Action for URL: {}", o.toString());
			log.info("Domain: {}, Property: {}", action.getDomain(Main.getDB().getDictionary()), action.getProperty(Main.getDB().getDictionary()));
			log.info("Future 1 with properties:");
					
			for (String s1 : future1properties ) {
				log.info(s1);
			}
			
			log.info("Boosting States which led us here...");
			
			for ( edu.kit.aifb.belt.db.QueryGraph.Edge edge1 : node.inEdges) {				
				QueryNode n1 = edge1.from;
				if (n1 != null) {
					// Interate over every past Binding. At least one of them led us here...
					for (Triple t2 : n1.getBindings()) {
						if (t2.o == t.s) {
							// Got i!
							log.info("The Triple {} {} {} {} led us here.", new Object[]{ ltbExecCxt.nodeDict.getNode(t2.s), ltbExecCxt.nodeDict.getNode(t2.p), ltbExecCxt.nodeDict.getNode(t2.o), t2});
							// boost it!
							// fake the node traversing :)
							QueryNode tmp = this.node;
							this.node = n1;
							learn(t2,depth+1);
							this.node = tmp;
						}
					}
				}
			}	
					
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

	}

	public Iterator<? extends Triple> getIterator() {
		return al.iterator();
	}
	
}
