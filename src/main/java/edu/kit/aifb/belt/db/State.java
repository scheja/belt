package edu.kit.aifb.belt.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

/**
 * A state in the decision graph.
 * 
 * @author sibbo
 */
public class State {
	private static final String SEPARATOR = "§";

	private Multiset<String> properties = HashMultiset.create();

	public State(Collection<String> properties) {
		this.properties.addAll(properties);
	}

	public State(String... properties) {
		this.properties.addAll(Arrays.asList(properties));
	}

	public Multiset<String> getProperties() {
		return properties;
	}

	public String toString() {
		List<Entry<String>> entries = new ArrayList<Entry<String>>(properties.entrySet().size());
		entries.addAll(properties.entrySet());
		Collections.sort(entries, EntryComparator.getComparator());
		StringBuilder str = new StringBuilder();

		for (Entry<String> s : entries) {
			str.append(s.getElement()).append(SEPARATOR);
		}

		return str.toString();
	}
}