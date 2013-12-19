package edu.kit.aifb.belt.sourceranker;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import edu.kit.aifb.belt.db.QueryGraph.Edge;
import edu.kit.aifb.belt.db.QueryGraph.QueryNode;
import edu.kit.aifb.belt.db.State;
import edu.kit.aifb.belt.db.StateChain;
import edu.kit.aifb.belt.db.StateFactory;
import edu.kit.aifb.belt.db.dict.StringDictionary;
import edu.kit.aifb.belt.searcher.Main;

public class RankPartialChainIterator {
	final private Logger log = LoggerFactory.getLogger( RankPartialChainIterator.class );
	private List<Triple> al;
	private LinkTraversalBasedExecutionContext ltbExecCxt;
	private SolutionMapping currentInputMapping;
	private TriplePattern currentQueryPattern;
	private TriplePattern tp;
	private QueryNode node;
	private QueryGraph queryGraph;

	public RankPartialChainIterator(Iterator<? extends Triple> currentMatches, LinkTraversalBasedExecutionContext ltbExecCxt, SolutionMapping currentInputMapping, TriplePattern currentQueryPattern, TriplePattern tp, QueryNode node, QueryGraph queryGraph) {
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
		
		List<Triple> unranked = new ArrayList<Triple>();
		while (currentMatches.hasNext()) {
			Triple t = currentMatches.next();
			node.addBinding(t);
			unranked.add(t);			
		}
		rank(unranked);
		
	}

	private void rank(List<Triple> unranked) {
		log.info("Here are {} triples which need to get ranked!", unranked.size());

		SourceRanker sr = new SourceRanker(Main.getDB());
		
		HashMap<String, List<Triple>> domain2triples = new HashMap<String, List<Triple>>();		
		
		for (Triple t : unranked) {			
			Node o = ltbExecCxt.nodeDict.getNode(t.o);
			
			try {
				URL url = new URL(o.toString());
				String domain = url.getHost();
				List<Triple> triplesFromDomain = domain2triples.get(domain);
				if (triplesFromDomain == null) {
					List<Triple> tmp = new ArrayList<Triple>();
					tmp.add(t);
					domain2triples.put(domain, tmp);
				} else {
					triplesFromDomain.add(t);
				}
			} catch (MalformedURLException e) {
			}
		}
		
		log.info("These triples come from {} different domains!", domain2triples.size());
				
		StateFactory statefactory = new StateFactory(Integer.MAX_VALUE, true);
		List<State> history1list = statefactory.createState(ltbExecCxt.nodeDict.getNode(currentQueryPattern.s).getURI(), Main.getDB());
		StateChain history = new StateChain(history1list);	
		
		System.out.println("History: H1: " + ltbExecCxt.nodeDict.getNode(currentQueryPattern.s).getURI());
		
		String actionProperty = ltbExecCxt.nodeDict.getNode(currentQueryPattern.p).getURI();
		
		log.info("Action property: {}", actionProperty);
		
		Multiset<String> future1multiset = HashMultiset.create();
		
		for (Edge edge : node.outEdges ) {
			QueryNode n = edge.to;
			if (n != null) {
				future1multiset.add(ltbExecCxt.nodeDict.getNode(n.getTriplePattern().p).getURI());
			}
		} 
		
		System.out.println("Future 1 Properties:");		
		for (String s : future1multiset) {
			System.out.println(s);
		}
		
		Set<String> domains = domain2triples.keySet();
		
		System.out.println("Unsorted Domains:");
		for (String s : domains) {
			System.out.println(s);
		}
				
		List<State> future1list = statefactory.createState(future1multiset, Main.getDB().getDictionary());
		StateChain future = new StateChain(future1list);	
		
		List<RankedDomain> rankedDomains = sr.rankSources(history, actionProperty, future, domains);
		
		System.out.println("Sorted Domains:");
		for (RankedDomain r : rankedDomains) {
			System.out.println(String.valueOf(r.getRank()) + ": " + r.getDomain());
			List<Triple> triplesfromdomain = domain2triples.get(r.getDomain());
			for (Triple t1 : triplesfromdomain) {
				al.add(t1);
			}
		}

				
	}

	public Iterator<? extends Triple> getIterator() {
		return al.iterator();
	}
	
}
