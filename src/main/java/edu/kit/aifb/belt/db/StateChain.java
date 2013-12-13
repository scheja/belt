package edu.kit.aifb.belt.db;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.kit.aifb.belt.db.dict.StringDictionary;

public class StateChain {
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

	public StateChain(InputStream in) {
		DataInputStream data = new DataInputStream(in);

		try {
			int size = data.readInt();

			for (int i = 0; i < size; i++) {
				states.add(new State(data));
			}
		} catch (IOException e) {
			// No actual IO involved.
			throw new RuntimeException(e);
		}
	}

	public List<State> getStateList() {
		return states;
	}

	public String toString() {
		StringBuilder str = new StringBuilder();

		for (State s : states) {
			str.append("[").append(s).append("], ");
		}

		return str.toString().substring(0, str.length() - 2);
	}

	public int size() {
		return states.size();
	}

	public byte[] getBytes() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(out);

		try {
			data.writeInt(states.size());

			for (State s : states) {
				s.getBytes(data);
			}
		} catch (IOException e) {
			// No actual IO involved.
			throw new RuntimeException(e);
		}

		return out.toByteArray();
	}
	
	public int hashCode() {
		int hash = 0;
		
		for (State s : states) {
			hash ^= s.hashCode();
		}
		
		return hash;
	}
	
	public boolean equals(Object o) {
		if (o instanceof StateChain) {
			StateChain c = (StateChain) o;
			
			if (c.states.size() != states.size()) {
				return false;
			}
			
			for (int i = 0; i < c.states.size(); i++) {
				if (!states.get(i).equals(c.states.get(i))) {
					return false;
				}
			}
			
			return true;
		} else {
			return false;
		}
	}
}