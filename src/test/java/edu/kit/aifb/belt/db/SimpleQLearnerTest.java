package edu.kit.aifb.belt.db;

import org.junit.Test;
import static org.junit.Assert.assertNotEquals;

import edu.kit.aifb.belt.learner.AbstractQLearner;
import edu.kit.aifb.belt.learner.SimpleQLearner;

public class SimpleQLearnerTest {
	@Test
	public void testQChanges() {
		Database db = new Database("janscheurenbrand.de/belt");	
		db.connect();
		AbstractQLearner learner = new SimpleQLearner(Long.MAX_VALUE, db);
		QValue q = new QValue(new StateChain(new State("a.de", "human", "school")), new Action("b.de", "hasFriend"), new StateChain(new State(null, "human", "school")));
		
		// Set q to zero.
		db.updateQ(q);
		
		learner.updateQ(q, 1, 0.5, 0.5);
		
		db.getQ(q);
		assertNotEquals("Q Value didn't change during learning process.", 0, q.getQ());
	}
}