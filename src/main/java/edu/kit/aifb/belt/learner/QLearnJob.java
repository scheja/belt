package edu.kit.aifb.belt.learner;

import edu.kit.aifb.belt.db.Action;
import edu.kit.aifb.belt.db.StateChain;

public class QLearnJob extends Job {
	private final String sourceURI;
	private final StateChain history;
	private final Action action;
	private final StateChain future;
	private final double learningRate;
	private final double discountFactor;
	private final boolean reward;

	public QLearnJob(String sourceURI, StateChain history, Action action, StateChain future, double learningRate,
			double discountFactor, boolean reward) {
		this.sourceURI = sourceURI;
		this.history = history;
		this.action = action;
		this.future = future;
		this.learningRate = learningRate;
		this.discountFactor = discountFactor;
		this.reward = reward;
	}

	public String getSourceURI() {
		return sourceURI;
	}

	public StateChain getHistory() {
		return history;
	}

	public Action getAction() {
		return action;
	}

	public StateChain getFuture() {
		return future;
	}

	public double getLearningRate() {
		return learningRate;
	}

	public double getDiscountFactor() {
		return discountFactor;
	}

	public boolean isReward() {
		return reward;
	}
}