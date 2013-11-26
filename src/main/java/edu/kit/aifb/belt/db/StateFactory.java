package edu.kit.aifb.belt.db;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Multiset;

import edu.kit.aifb.belt.db.dict.StringDictionary;

public class StateFactory extends AbstractStateFactory {
	private int maxProps;
	private boolean ignoreMultiProps;

	/**
	 * @param maxProps The maximum number of properties per state. Amount of
	 *            states raises exponentially in dependence of this number, so
	 *            be careful.
	 * @param ignoreMultiProps If true, the multiset will be seen as a normal
	 *            set, meaning that multiple properties will be reduced to one.
	 */
	public StateFactory(int maxProps, boolean ignoreMultiProps) {
		this.maxProps = maxProps;
		this.ignoreMultiProps = ignoreMultiProps;
	}

	@Override
	public List<State> createState(String domain, String type, Multiset<String> properties, StringDictionary dict) {
		if (ignoreMultiProps) {
			for (String entry : properties.elementSet()) {
				properties.setCount(entry, 1);
			}
		}

		List<State> states = new ArrayList<State>();

		if (properties.size() > maxProps) {
			String[] props = properties.toArray(new String[properties.size()]);
			Combination<String> comb = new Combination<String>(props);
			List<Multiset<String>> propSets = comb.combinations(maxProps);
			
			for (Multiset<String> propSet : propSets) {
				states.add(new State(domain, type, propSet, dict));
			}
		} else {
			states.add(new State(domain, type, properties, dict));
		}

		return states;
	}
}