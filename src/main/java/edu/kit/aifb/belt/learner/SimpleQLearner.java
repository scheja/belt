package edu.kit.aifb.belt.learner;

import edu.kit.aifb.belt.db.Action;
import edu.kit.aifb.belt.db.Database;
import edu.kit.aifb.belt.db.QValue;
import edu.kit.aifb.belt.db.State;
import edu.kit.aifb.belt.db.StateChain;

public class SimpleQLearner extends AbstractQLearner {
	private long maxDbSize;
	private Database db;
	private DatabaseListener listener;
	private volatile boolean listenerInformed = false;

	public SimpleQLearner(long maxDbSize, Database db, DatabaseListener listener) {
		this.maxDbSize = maxDbSize;
		this.db = db;
		this.listener = listener;
	}

	protected void updateQInternal(String sourceURI, StateChain history,
			Action action, StateChain future, double learningRate,
			double discountFactor, boolean isReward) {
		double reward;
		
		if (isReward) {
			reward = getRewardFromSourceURI(sourceURI, db);
		} else {
			reward = getNegativeAverageReward(db);
		}

		if (db.getSize() > maxDbSize) {
			if (!listenerInformed && listener != null) {
				listenerInformed = true;
				listener.databaseFull();
			}

			return;
		}

		if (history.size() < 1 || future.size() < 1) {
			throw new IllegalArgumentException(
					"Cannot update Q without a start and an end state.");
		}

		QValue q = new QValue(history, action, future);
		// Get q. If it doesn't exist, it will be reward.
		if (!db.getQ(q)) {
			q.setQ(reward);
		}

		// Create new state list.
		StateChain newHistory = new StateChain(future.getStateList().get(0));
		// Add first element of future states.
		//newHistory.getStateList().add(future.getStateList().get(0));

		double bestFutureQ = db.getBestQ(newHistory);

		if (Double.isNaN(bestFutureQ)) {
			bestFutureQ = 0;
		}

		q.setQ(q.getQ() + learningRate
				* (reward + discountFactor * bestFutureQ - q.getQ()));

		db.updateQ(q);
	}
}