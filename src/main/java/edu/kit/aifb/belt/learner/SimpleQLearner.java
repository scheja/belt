package edu.kit.aifb.belt.learner;

import edu.kit.aifb.belt.db.Action;
import edu.kit.aifb.belt.db.Database;
import edu.kit.aifb.belt.db.QValue;
import edu.kit.aifb.belt.db.StateChain;

public class SimpleQLearner extends AbstractQLearner {
	private long maxDbSize;
	private Database db;

	public SimpleQLearner(long maxDbSize, Database db) {
		this.maxDbSize = maxDbSize;
		this.db = db;
	}
	
	
	public boolean updateQ(StateChain history, Action action, StateChain future, double reward, double learningRate, double discountFactor) {
		if (db.getSize() > maxDbSize) {
			return false;
		}
		
		if (history.size() < 1 || future.size() < 1) {
			throw new IllegalArgumentException("Cannot update Q without a start and an end state.");
		}
		
		
		
		QValue q = new QValue(history, action, future);
		// Get q. If it doesn't exist, it will be 0.
		db.getQ(q);
		
		StateChain newHistory = new StateChain(history);
		// Add first element of future states.
		newHistory.getStateList().add(future.getStateList().get(0));
		
		double bestFutureQ = db.getBestQ(newHistory);
		
		if (Double.isNaN(bestFutureQ)) {
			bestFutureQ = 0;
		}
		
		q.setQ(q.getQ() + learningRate * (reward + discountFactor * bestFutureQ - q.getQ()));
		
		db.updateQ(q);
		
		return true;
	}
}