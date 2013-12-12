package edu.kit.aifb.belt.sourceranker;

import java.util.Comparator;

import edu.kit.aifb.belt.db.QValue;

public class QValueComparator implements Comparator<QValue> {
	public int compare(QValue o1, QValue o2) {
		if (o1.getQ() > o2.getQ()) {
			return 1;
		} else if (o1.getQ() < o2.getQ()) {
			return -1;
		} else {
			final int hash1 = o1.hashCode();
			final int hash2 = o2.hashCode();

			if (hash1 > hash2) {
				return 1;
			} else if (hash1 < hash2) {
				return -1;
			} else {
				return 0;
			}
		}
	}
}