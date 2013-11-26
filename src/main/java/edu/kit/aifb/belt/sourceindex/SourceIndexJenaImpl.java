package edu.kit.aifb.belt.sourceindex;

import java.util.Iterator;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.RDFList.ReduceFn;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.base.file.Location;

import edu.kit.aifb.belt.learner.Main;

public class SourceIndexJenaImpl implements SourceIndex {
	static Logger l = LoggerFactory.getLogger(Main.class);
	private Location location;
	private Dataset dataset;
	private DatasetGraph dsg;
	static Stack<Redirection> redirections = new Stack<Redirection>();

	public SourceIndexJenaImpl() {
		location = new Location("tdb/dataset2");
		dataset = TDBFactory.createDataset(location);
		dsg = dataset.asDatasetGraph();
	}

	public void addQuad(Node g, Node s, Node p, Node o) {
		dataset.begin(ReadWrite.WRITE);
		try {
			dsg.add(new Quad(g, s, p, o));
			dataset.commit();
		} finally {
			dataset.end();
		}

	}

	public Iterator<Quad> findAllByURI(String uri) {
		Iterator<Quad> res = null;
		dataset.begin(ReadWrite.READ);
		try {
			res = dsg.find(Node.createURI(uri), Node.ANY, Node.ANY, Node.ANY);
		} finally {
			dataset.end();
		}
		return res;
	}

	public void test() {
		dsg.add(new Quad(Node.createURI("a"), Node.createURI("s1"), Node
				.createURI("p1"), Node.createURI("o1")));
		dsg.add(new Quad(Node.createURI("a"), Node.createURI("s2"), Node
				.createURI("p2"), Node.createURI("o2")));
		dsg.add(new Quad(Node.createURI("a"), Node.createURI("s3"), Node
				.createURI("p3"), Node.createURI("o3")));
		dsg.add(new Quad(Node.createURI("b"), Node.createURI("s1"), Node
				.createURI("p1"), Node.createURI("o1")));
		dsg.add(new Quad(Node.createURI("b"), Node.createURI("s2"), Node
				.createURI("p2"), Node.createURI("o2")));
		dsg.add(new Quad(Node.createURI("b"), Node.createURI("s3"), Node
				.createURI("p3"), Node.createURI("o3")));

		Iterator<Quad> res = findAllByURI("a");
		while (res.hasNext()) {
			System.out.println(res.next());
		}

	}

	public static void addRedirect(String from, String to) {
		l.debug("Added Redirect from <{}> to <{}>", from, to);
		redirections.push(new Redirection(from, to));
	}
	
	public static void handleRedirections() {
		l.info("Now handling Redirections");
		SourceIndexJenaImpl si = new SourceIndexJenaImpl();
		while (!redirections.empty()) {
			Redirection r = redirections.pop();
			si.updateURIs(r.from, r.to);
		}
	}

	
	public void updateURIs(String from, String to){
		l.debug("Updating URIs from <{}> to <{}>", from, to);
		Iterator<Quad> foundUnderTo = findAllByURI(to);
		int counter = 0;
		while(foundUnderTo.hasNext()) {			
			Node g = Node.createURI(from);
			Quad q = foundUnderTo.next();
			addQuad(g, q.getSubject(), q.getPredicate(), q.getObject());
			counter ++;
		}
		l.debug("Handled <{}> triples.", counter);
	} 

	static class Redirection { 
		  public final String from; 
		  public final String to; 
		  public Redirection(String from, String to	) { 
		    this.from = from; 
		    this.to = to; 
		  } 
		} 
}
