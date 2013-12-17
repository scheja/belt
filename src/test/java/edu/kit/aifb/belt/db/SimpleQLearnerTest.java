package edu.kit.aifb.belt.db;

import static org.junit.Assert.assertNotEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Quad;

import edu.kit.aifb.belt.learner.AbstractQLearner;
import edu.kit.aifb.belt.learner.DatabaseListener;
import edu.kit.aifb.belt.learner.SimpleQLearner;

public class SimpleQLearnerTest implements DatabaseListener {
	@Test
	public void testQChanges() {
		Database db = new Database();
		db.connect();
		AbstractQLearner learner = new SimpleQLearner(Long.MAX_VALUE, db, this);
		learner.start();

		Set<String> humanSet = new HashSet<String>();
		humanSet.add("human");

		QValue q = new QValue(new StateChain(new State("a.de", humanSet, db.getDictionary(), "school")), new Action(
				"b.de", "hasFriend", db.getDictionary()), new StateChain(new State(null, humanSet, db.getDictionary(),
				"school")));

		// Set q to zero.
		db.updateQ(q);

		Quad source = new Quad(Node.createURI("c"), Node.createURI("s"), Node.createURI("p"), Node.createURI("o"));
		db.addQuad(source);

		for (int i = 0; i < 4; i++) {
			learner.updateQ(source.getGraph().toString(), q, 0.5, 0.5, true);
		}

		Thread.yield();
		learner.stop();

		db.getQ(q);
		assertNotEquals("Q value didn't change during learning process.", 0, q.getQ());

		db.close();
	}

	public void databaseFull() {
		System.out.println("Database full");
	}
}