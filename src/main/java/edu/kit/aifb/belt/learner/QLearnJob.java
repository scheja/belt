package edu.kit.aifb.belt.learner;

import edu.kit.aifb.belt.db.Action;
import edu.kit.aifb.belt.db.StateChain;

public class QLearnJob extends Job {
	private StateChain history;
	private Action action;
	private StateChain future;
	private double reward;
	private double learningRate;
	private double discountFactor;
	
	public QLearnJob(StateChain history, Action action, StateChain future, double reward, double learningRate, double discountFactor){
		this.history = history;
		this.action = action;
		this.future = future;
		this.reward = reward;
		this.learningRate = learningRate;
		this.discountFactor = discountFactor;
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

	public double getReward() {
		return reward;
	}

	public double getLearningRate() {
		return learningRate;
	}

	public double getDiscountFactor() {
		return discountFactor;
	}
}