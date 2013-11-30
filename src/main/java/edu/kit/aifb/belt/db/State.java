package edu.kit.aifb.belt.db;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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
	private int[] type;

	public State(String domain, Set<String> type, Collection<String> properties, StringDictionary dict) {
		this.properties = new int[properties.size()];

		int i = 0;
		for (String s : properties) {
			this.properties[i++] = dict.getId(s);
		}

		Arrays.sort(this.properties);

		this.domain = dict.getId(domain);

		if (type == null) {
			this.type = new int[0];
		} else {
			this.type = new int[type.size()];

			i = 0;
			for (String s : type) {
				this.type[i++] = dict.getId(s);
			}

			Arrays.sort(this.type);
		}
	}

	public State(String domain, Set<String> type, StringDictionary dict, String... properties) {
		this(domain, type, Arrays.asList(properties), dict);
	}

	public State(int domain, int[] type, int[] properties) {
		this.domain = domain;
		this.type = type;
		this.properties = properties;

		Arrays.sort(this.properties);
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

	public Set<String> getType(StringDictionary dict) {
		Set<String> result = new HashSet<String>();

		for (int i : type) {
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

	public void getBytes(DataOutputStream data, StringDictionary stringDict) throws IOException {
		data.writeInt(type.length);

		for (int i : type) {
			data.writeInt(i);
		}

		data.writeInt(domain);

		data.writeInt(properties.length);

		for (int i : properties) {
			data.writeInt(i);
		}
	}
}