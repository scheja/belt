package edu.kit.aifb.belt.learner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squin.dataset.Triple;
import org.squin.dataset.jenacommon.NodeDictionary;

import com.hp.hpl.jena.graph.Node;

public class LearnPartialChainIterator {
	private List<Triple> al;
	final private Logger log = LoggerFactory.getLogger( LearnPartialChainIterator.class );

	public LearnPartialChainIterator(Iterator<? extends Triple> currentMatches, NodeDictionary nodeDict) {
		al = new ArrayList<Triple>();
		while (currentMatches.hasNext()) {
			Triple t = currentMatches.next();
			al.add(t);
			Node s = nodeDict.getNode(t.s);
			Node p = nodeDict.getNode(t.p);
			Node o = nodeDict.getNode(t.o);
			log.info("Match: <{}> (ID: <{}>) / <{}> (ID: <{}>) / <{}> (ID: <{}>)", new Object[]{s, t.s, p, t.p, o , t.o});
		}
	}

	public Iterator<? extends Triple> getIterator() {
		return al.iterator();
	}
	
}