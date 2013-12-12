package edu.kit.aifb.belt.db;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import edu.kit.aifb.belt.db.dict.StringDictionary;

/**
 * A state in the decision graph.
 * 
 * @author sibbo
 */
public class State {
	private int[] properties;
	private int domain;
	private int[] types;

	public State(String domain, Set<String> types, Collection<String> properties, StringDictionary dict) {
		Objects.requireNonNull(dict, "Dictionary must not be null");
		
		this.properties = new int[properties.size()];

		int i = 0;
		for (String s : properties) {
			this.properties[i++] = dict.getId(s);
		}

		Arrays.sort(this.properties);

		this.domain = dict.getId(domain);

		if (types == null) {
			this.types = new int[0];
		} else {
			this.types = new int[types.size()];

			i = 0;
			for (String s : types) {
				this.types[i++] = dict.getId(s);
			}

			Arrays.sort(this.types);
		}
	}

	public State(String domain, Set<String> types, StringDictionary dict, String... properties) {
		this(domain, types, Arrays.asList(properties), dict);
	}

	public State(int domain, int[] types, int[] properties) {
		this.domain = domain;
		this.types = types;
		this.properties = properties;

		Arrays.sort(this.types);
		Arrays.sort(this.properties);
	}

	public State(DataInputStream data) throws IOException {
		types = new int[data.readInt()];

		for (int i = 0; i < types.length; i++) {
			types[i] = data.readInt();
		}

		domain = data.readInt();

		properties = new int[data.readInt()];

		for (int i = 0; i < properties.length; i++) {
			properties[i] = data.readInt();
		}
	}

	public Multiset<String> getProperties(StringDictionary dict) {
		Multiset<String> set = HashMultiset.create();

		for (int l : properties) {
			set.add(dict.getString(l));
		}

		return set;
	}

	public String getDomain(StringDictionary dict) {
		return dict.getString(domain);
	}

	public Set<String> getTypes(StringDictionary dict) {
		Set<String> result = new HashSet<String>();

		for (int i : types) {
			result.add(dict.getString(i));
		}

		return result;
	}

	/**
	 * Returns a copy of this state without domain and type.
	 * 
	 * @return A copy of this state without domain and type.
	 */
	public State getCleanCopy() {
		return new State(0, null, properties);
	}

	public void getBytes(DataOutputStream data) throws IOException {
		data.writeInt(types.length);

		for (int i : types) {
			data.writeInt(i);
		}

		data.writeInt(domain);

		data.writeInt(properties.length);

		for (int i : properties) {
			data.writeInt(i);
		}
	}

	public int hashCode() {
		return domain ^ Arrays.hashCode(types) ^ Arrays.hashCode(properties);
	}

	public boolean equals(Object o) {
		if (o instanceof State) {
			State s = (State) o;

			return s.domain == domain && Arrays.equals(s.types, types) && Arrays.equals(s.properties, properties);
		} else {
			return false;
		}
	}

	public String toString() {
		StringBuilder str = new StringBuilder();

		str.append("Domain: ").append(domain);
		str.append(" Type: ").append(Arrays.toString(types));
		str.append(" Properties: ").append(Arrays.toString(properties));

		return str.toString();
	}

	public int[] getProperties() {
		return properties;
	}

	public int[] getTypes() {
		return types;
	}
}