package edu.kit.aifb.belt.db;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import edu.kit.aifb.belt.learner.AbstractQLearner;
import edu.kit.aifb.belt.learner.DatabaseListener;
import edu.kit.aifb.belt.learner.SimpleQLearner;

public class SimpleQLearnerTest implements DatabaseListener {
	@Test
	public void testQChanges() {
		Database db = new Database("janscheurenbrand.de/belt");	
		db.connect();
		AbstractQLearner learner = new SimpleQLearner(Long.MAX_VALUE, db, this);
		learner.start();
		QValue q = new QValue(new StateChain(new State("a.de", "human", db.getDictionary(), "school")), new Action("b.de", "hasFriend", db.getDictionary()), new StateChain(new State(null, "human",db.getDictionary(), "school")));
		
		// Set q to zero.
		db.updateQ(q);
		
		learner.updateQ(q, 1, 0.5, 0.5);
		
		db.getQ(q);
		assertNotEquals("Q Value didn't change during learning process.", 0, q.getQ());
		learner.stop();
		db.close();
	}

	public void databaseFull() {
		System.out.println("Database full");
	}
}