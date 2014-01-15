package edu.kit.aifb.belt.db;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;

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
	private SummaryStatistics statistics;

	public QValue(StateChain history, Action action, StateChain future) {
		this(history, action, future, 0);
	}

	public QValue(StateChain history, Action action, StateChain future, double q) {
		this(history, action, future, q, new SummaryStatistics());
	}

	public QValue(StateChain history, Action action, StateChain future, double q, SummaryStatistics statistics) {
		this.history = history;
		this.action = action;
		this.future = future;
		this.q = q;
		this.statistics = statistics;
	}

	public QValue(QValue qValue) {
		history = qValue.history;
		action = qValue.action;
		future = qValue.future;
		q = qValue.q;
		statistics = qValue.statistics;
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
		return (history.hashCode() << 1) ^ action.hashCode() ^ (future.hashCode() >> 1) ^ ((int) Double.doubleToRawLongBits(q)) ^ statistics.hashCode();
	}
	
	public boolean equals(Object o) {
		if (o instanceof QValue) {
			QValue q = (QValue) o;
			
			boolean result =  q.history.equals(history) && q.action.equals(action) && q.future.equals(future) && q.q == this.q;
			
			if (this.statistics != null) {
				result &= statistics.equals(q.statistics);
			} else {
				result &= this.statistics == null && q.statistics == null;
			}
			
			return result;
		} else {
			return false;
		}
	}
	
	public String toString() {
		return "History: " + history + " Action: " + action + " Future: " + future + " Q: " + q + " statistics: " + statistics;
	}

	public SummaryStatistics getStatistics() {
		return statistics;
	}

	public void setStatistics(SummaryStatistics statistics) {
		this.statistics = statistics;
	}
}