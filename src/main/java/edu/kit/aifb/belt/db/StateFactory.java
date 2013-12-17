package edu.kit.aifb.belt.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.Multiset;

import edu.kit.aifb.belt.db.dict.StringDictionary;

public class StateFactory extends AbstractStateFactory {
	private final int maxProps;
	private final boolean ignoreMultiProps;
	private final boolean doKSample;

	public StateFactory(int maxProps, boolean ignoreMultiProps) {
		this(maxProps, ignoreMultiProps, true);
	}

	public StateFactory(int maxProps) {
		this(maxProps, true, true);
	}

	/**
	 * @param maxProps
	 *            The maximum number of properties per state. Amount of states
	 *            raises exponentially in dependence of this number, so be
	 *            careful.
	 * @param ignoreMultiProps
	 *            If true, the multiset will be seen as a normal set, meaning
	 *            that multiple properties will be reduced to one.
	 */
	public StateFactory(int maxProps, boolean ignoreMultiProps,
			boolean doKSample) {
		this.maxProps = maxProps;
		this.ignoreMultiProps = ignoreMultiProps;
		this.doKSample = doKSample;
	}

	@Override
	public List<State> createState(String domain, Set<String> type,
			Multiset<String> properties, StringDictionary dict) {
		if (ignoreMultiProps) {
			for (String entry : properties.elementSet()) {
				properties.setCount(entry, 1);
			}
		}

		List<State> states = new ArrayList<State>();

		if (doKSample) {
			Random r = new Random();
			Set<String> props = new TreeSet<String>();
			List<String> allProps = new ArrayList<String>(properties.size());
			allProps.addAll(properties);

			if (properties.size() > maxProps) {
				for (int i = 0; i < maxProps; i++) {
					props.add(allProps.remove(r.nextInt(allProps.size())));
				}
			} else {
				states.add(new State(domain, type, properties, dict));
			}
		} else {
			if (properties.size() > maxProps) {
				String[] props = properties.toArray(new String[properties
						.size()]);
				Combination<String> comb = new Combination<String>(props);
				List<Multiset<String>> propSets = comb.combinations(maxProps);

				for (Multiset<String> propSet : propSets) {
					states.add(new State(domain, type, propSet, dict));
				}
			} else {
				states.add(new State(domain, type, properties, dict));
			}
		}

		return states;
	}
}