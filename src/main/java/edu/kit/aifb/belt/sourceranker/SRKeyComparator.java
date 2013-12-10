package edu.kit.aifb.belt.sourceranker;

import java.util.Comparator;

public class SRKeyComparator implements Comparator<SRKey> {
	public int compare(SRKey t1, SRKey t2) {
		// First action then pap, because we will search for the same action
		// multiple times. This way, the first part of the path in the tree will
		// be always the same and the nodes will be cached.
		if (t2.getAction() != t1.getAction()) {
			return t2.getAction() > t1.getAction() ? 1 : -1;
		} else {
			if (t2.getPropertyAndPosition() != t1.getPropertyAndPosition()) {
				return t2.getPropertyAndPosition() > t1.getPropertyAndPosition() ? 1 : -1;
			} else {
				return 0;
			}
		}

	}
}