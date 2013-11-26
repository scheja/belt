package edu.kit.aifb.belt.db;

public abstract class AbstractQLearner {
	public abstract boolean updateQ(StateChain history, Action action, StateChain future, double reward, double learningRate, double discountFactor);
}