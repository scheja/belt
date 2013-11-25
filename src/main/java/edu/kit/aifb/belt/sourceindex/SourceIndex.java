package edu.kit.aifb.belt.sourceindex;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.base.file.Location;

import edu.kit.aifb.belt.learner.Main;

public class SourceIndex {
	static Logger l = LoggerFactory.getLogger(Main.class);
	private Location location;
	private Dataset dataset;
	private DatasetGraph dsg;

	public SourceIndex() {
		location = new Location("tdb/dataset1");
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
}
