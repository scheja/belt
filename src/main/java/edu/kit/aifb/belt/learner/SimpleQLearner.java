package edu.kit.aifb.belt.learner;

import edu.kit.aifb.belt.db.Action;
import edu.kit.aifb.belt.db.Database;
import edu.kit.aifb.belt.db.QValue;
import edu.kit.aifb.belt.db.Quality;
import edu.kit.aifb.belt.db.State;
import edu.kit.aifb.belt.db.StateChain;

public class SimpleQLearner extends AbstractQLearner {
	private long maxDbSize;
	private Database db;
	private DatabaseListener listener;
	private volatile boolean listenerInformed = false;
	private QualityMeasurementType type;

	public SimpleQLearner(long maxDbSize, Database db,
			DatabaseListener listener, QualityMeasurementType type) {
		this.maxDbSize = maxDbSize;
		this.db = db;
		this.listener = listener;
		this.type = type;
	}

	protected void updateQInternal(String sourceURI, StateChain history,
			Action action, StateChain future, double learningRate,
			double discountFactor, boolean isReward) {
		Quality quality;
		double reward;

		if (isReward) {
			quality = getRewardFromSourceURI(sourceURI, db);
		} else {
			quality = getNegativeAverageReward(db);
		}

		switch (type) {
		case UNIFORM:
			reward = quality.getUniform();
			break;
		case NORMAL:
			reward = quality.getNormal();
			break;
		case EXPONENTIAL:
			reward = quality.getExponential();
			break;
		default:
			throw new RuntimeException("Unknown quality type: " + type);
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
		// Get q. If it doesn't exist, it will be 0.
		db.getQ(q);

		StateChain newHistory = new StateChain(history);
		// Add first element of future states.
		newHistory.getStateList().add(future.getStateList().get(0));

		double bestFutureQ = db.getBestQ(newHistory);

		if (Double.isNaN(bestFutureQ)) {
			bestFutureQ = 0;
		}

		q.setQ(q.getQ() + learningRate
				* (reward + discountFactor * bestFutureQ - q.getQ()));

		db.updateQ(q);
	}
}