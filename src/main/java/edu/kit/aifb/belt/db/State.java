package edu.kit.aifb.belt.db;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import edu.kit.aifb.belt.db.dict.StringDictionary;

/**
 * A state in the decision graph.
 * 
 * @author sibbo
 */
public class State {
	private long[] properties;
	private long domain;
	private long type;

	public State(String domain, String type, Collection<String> properties, StringDictionary dict) {
		this.properties = new long[properties.size()];

		int i = 0;
		for (String s : properties) {
			this.properties[i++] = dict.getId(s);
		}

		Arrays.sort(this.properties);

		this.domain = dict.getId(domain);
		this.type = dict.getId(type);
	}

	public State(String domain, String type, StringDictionary dict, String... properties) {
		this(domain, type, Arrays.asList(properties), dict);
	}

	public State(long domain, long type, long[] properties) {
		this.domain = domain;
		this.type = type;
		this.properties = properties;
		
		Arrays.sort(this.properties);
	}

	public Multiset<String> getProperties(StringDictionary dict) {
		Multiset<String> set = HashMultiset.create();

		for (long l : properties) {
			set.add(dict.getString(l));
		}
		
		return set;
	}

	public String getDomain(StringDictionary dict) {
		return dict.getString(domain);
	}

	public String getType(StringDictionary dict) {
		return dict.getString(type);
	}

	/**
	 * Returns a copy of this state without domain and type.
	 * 
	 * @return A copy of this state without domain and type.
	 */
	public State getCleanCopy() {
		return new State(0, 0, properties);
	}

	public void getBytes(DataOutputStream data, StringDictionary stringDict) throws IOException {
		data.writeLong(type);
		data.writeLong(domain);

		data.writeInt(properties.length);

		for (long l : properties) {
			data.writeLong(l);
		}
	}
}