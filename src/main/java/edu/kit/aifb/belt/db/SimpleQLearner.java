package edu.kit.aifb.belt.db;

public class SimpleQLearner extends AbstractQLearner {
	private long maxDbSize;
	private Database db;

	public SimpleQLearner(long maxDbSize, Database db) {
		this.maxDbSize = maxDbSize;
		this.db = db;
	}
	
	/**
	 * Performs an update q operation.
	 * @param history The state history. Entries should have a domain and a type.
	 * @param action The action.
	 * @param future The state future. Entries shouldn't have a domain, but a type.
	 */
	public boolean updateQ(StateChain history, Action action, StateChain future, double reward, double learningRate, double discountFactor) {
		if (db.getSize() > maxDbSize) {
			return false;
		}
		
		if (history.size() < 1 || future.size() < 1) {
			throw new IllegalArgumentException("Cannot update Q without a start and an end state.");
		}
		
		
		
		QValue q = new QValue(history, action, future);
		// Get q. If it doesn't exist, it will be 0
		db.getQ(q);
		
		StateChain newHistory = new StateChain(history);
		// Add first last element of future states.
		newHistory.getStateList().add(future.getStateList().get(0));
		q.setQ(q.getQ() + learningRate * (reward + discountFactor * db.getBestQ(newHistory) - q.getQ()));
		
		db.updateQ(q);
		
		return true;
	}
}