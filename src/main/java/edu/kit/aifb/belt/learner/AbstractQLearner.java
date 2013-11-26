package edu.kit.aifb.belt.learner;

import edu.kit.aifb.belt.db.Action;
import edu.kit.aifb.belt.db.QValue;
import edu.kit.aifb.belt.db.StateChain;

public abstract class AbstractQLearner {
	public abstract boolean updateQ(StateChain history, Action action, StateChain future, double reward, double learningRate, double discountFactor);
	
	public boolean updateQ(QValue q, double reward, double learningRate, double discountFactor) {
		return updateQ(q.getHistory(), q.getAction(), q.getFuture(), reward, learningRate, discountFactor);
	}
}