package edu.kit.aifb.belt.learner;

import edu.kit.aifb.belt.db.Action;
import edu.kit.aifb.belt.db.StateChain;

public class QLearnJob extends Job {
	private String sourceURI;
	private StateChain history;
	private Action action;
	private StateChain future;
	private double learningRate;
	private double discountFactor;

	public QLearnJob(String sourceURI, StateChain history, Action action, StateChain future, double learningRate,
			double discountFactor) {
		this.sourceURI = sourceURI;
		this.history = history;
		this.action = action;
		this.future = future;
		this.learningRate = learningRate;
		this.discountFactor = discountFactor;
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
}