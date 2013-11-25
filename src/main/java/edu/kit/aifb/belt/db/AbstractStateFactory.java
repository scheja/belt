package edu.kit.aifb.belt.db;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public abstract class AbstractStateFactory {
	public abstract List<State> createState(String domain, String type, Multiset<String> properties);

	public List<State> createState(String domain, String type, String... properties) {
		Multiset<String> set = HashMultiset.create();
		set.addAll(Arrays.asList(properties));

		return createState(domain, type, set);
	}

	public List<State> createState(Multiset<String> properties) {
		return createState(null, null, properties);
	}

	public List<State> createState(String... properties) {
		return createState(null, null, properties);
	}
}