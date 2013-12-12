package edu.kit.aifb.belt.sourceranker;

public class RankedDomain implements Comparable<RankedDomain> {
	private final String domain;
	private final double rank;
	
	public RankedDomain(String domain, double rank) {
		this.domain = domain;
		this.rank = rank;
	}
	
	public int compareTo(RankedDomain o) {
		if (rank < o.rank) {
			return -1;
		} else if (rank == o.rank) {
			return 0;
		} else {
			return 1;
		}
	}

	public String getDomain() {
		return domain;
	}

	public double getRank() {
		return rank;
	}
}