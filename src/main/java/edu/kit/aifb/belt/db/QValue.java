package edu.kit.aifb.belt.db;

/**
 * A tuple of history state chain, action and future state chain mapped to a q
 * value.
 * 
 * @author sibbo
 */
public class QValue {
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
	
	public int hashCode() {
		return (history.hashCode() << 1) ^ action.hashCode() ^ (future.hashCode() >> 1) ^ ((int) Double.doubleToRawLongBits(q));
	}
	
	public boolean equals(Object o) {
		if (o instanceof QValue) {
			QValue q = (QValue) o;
			
			return q.history.equals(history) && q.action.equals(action) && q.future.equals(future) && q.q == this.q;
		} else {
			return false;
		}
	}
	
	public String toString() {
		return "History: " + history + " Action: " + action + " Future: " + future + " Q: " + q;
	}
}