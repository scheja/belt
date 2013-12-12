package edu.kit.aifb.belt.sourceranker;

import java.util.Comparator;

public class SRResultValueSimilarityComparator implements Comparator<SRResultValue> {
	public int compare(SRResultValue o1, SRResultValue o2) {
		if (o1.getSimilarity() < o2.getSimilarity()) {
			return 1;
		} else if (o1.getSimilarity() == o2.getSimilarity()) {
			return 0;
		} else {
			return -1;
		}
	}
}