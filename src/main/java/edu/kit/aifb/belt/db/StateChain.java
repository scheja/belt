package edu.kit.aifb.belt.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StateChain {
	private static final String SEPARATOR = "§";

	private List<State> states = new ArrayList<State>();

	public StateChain(StateChain states) {
		this(states.getStateList());
	}
	
	public StateChain(List<State> states) {
		this.states.addAll(states);
	}

	public StateChain(State... states) {
		this.states.addAll(Arrays.asList(states));
	}

	public List<State> getStateList() {
		return states;
	}

	public String toString() {
		StringBuilder str = new StringBuilder();

		for (State s : states) {
			str.append(s).append(SEPARATOR);
		}

		return str.toString();
	}

	public int size() {
		return states.size();
	}
}