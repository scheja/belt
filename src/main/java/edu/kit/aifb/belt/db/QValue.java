package edu.kit.aifb.belt.db;

/**
 * A tuple of history state chain, action and future state chain mapped to a q
 * value.
 * 
 * @author sibbo
 */
public class QValue {
	private static final String SEPARATOR = "§";

	private StateChain history;
	private Action action;
	private StateChain future;
	private double q;

	public QValue(StateChain history, Action action, StateChain future) {
		this.history = history;
		this.action = action;
		this.future = future;
	}

	public QValue(StateChain history, Action action, StateChain future, double q) {
		this.history = history;
		this.action = action;
		this.future = future;
		this.q = q;
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

	public double getQ() {
		return q;
	}

	public void setQ(double q) {
		this.q = q;
	}
}