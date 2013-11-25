package edu.kit.aifb.belt.db;

import java.util.Comparator;

import com.google.common.collect.Multiset.Entry;

public class EntryComparator implements Comparator<Entry<String>> {
	private static final EntryComparator instance = new EntryComparator();

	public static Comparator<Entry<String>> getComparator() {
		return instance;
	}
	
	public int compare(Entry<String> o1, Entry<String> o2) {
		return o1.getElement().compareTo(o2.getElement());
	}
}