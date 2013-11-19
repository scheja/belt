package edu.kit.aifb.belt.db;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

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
	
	public Multiset<String> getProperties() {
		return properties;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		
		for (String s: properties) {
			str.append(s).append(SEPARATOR);
		}
		
		return str.toString();
	}
}