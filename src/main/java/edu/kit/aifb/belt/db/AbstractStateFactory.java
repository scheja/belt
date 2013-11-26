package edu.kit.aifb.belt.db;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import edu.kit.aifb.belt.db.dict.StringDictionary;

public abstract class AbstractStateFactory {
	public abstract List<State> createState(String domain, String type, Multiset<String> properties, StringDictionary dict);

	public List<State> createState(String domain, String type, StringDictionary dict, String... properties) {
		Multiset<String> set = HashMultiset.create();
		set.addAll(Arrays.asList(properties));

		return createState(domain, type, set, dict);
	}

	public List<State> createState(Multiset<String> properties, StringDictionary dict) {
		return createState(null, null, properties, dict);
	}

	public List<State> createState(StringDictionary dict, String... properties) {
		return createState(null, null, dict, properties);
	}
}