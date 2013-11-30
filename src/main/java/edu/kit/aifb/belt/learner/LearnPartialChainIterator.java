package edu.kit.aifb.belt.learner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squin.dataset.Triple;
import org.squin.dataset.jenacommon.NodeDictionary;
import org.squin.dataset.query.SolutionMapping;
import org.squin.dataset.query.TriplePattern;
import org.squin.dataset.query.impl.FixedSizeSolutionMappingImpl;
import org.squin.engine.LinkTraversalBasedExecutionContext;

import com.hp.hpl.jena.graph.Node;

public class LearnPartialChainIterator {
	final private Logger log = LoggerFactory.getLogger( LearnPartialChainIterator.class );
	private List<Triple> al;
	private LinkTraversalBasedExecutionContext ltbExecCxt;
	private SolutionMapping currentInputMapping;
	private TriplePattern currentQueryPattern;


	public LearnPartialChainIterator(Iterator<? extends Triple> currentMatches, LinkTraversalBasedExecutionContext ltbExecCxt, SolutionMapping currentInputMapping, TriplePattern currentQueryPattern) {
		al = new ArrayList<Triple>();
		this.ltbExecCxt = ltbExecCxt;
		this.currentInputMapping = currentInputMapping;
		this.currentQueryPattern = currentQueryPattern;
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
		log.info("Match: <{}> (n{}) / <{}> (n{}) / <{}> (n{})", new Object[]{s, t.s, p, t.p, o , t.o});		
	}

	public Iterator<? extends Triple> getIterator() {
		return al.iterator();
	}
	
}