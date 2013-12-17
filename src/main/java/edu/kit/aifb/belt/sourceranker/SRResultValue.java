package edu.kit.aifb.belt.sourceranker;

import com.googlecode.javaewah.EWAHCompressedBitmap;

import edu.kit.aifb.belt.db.Action;
import edu.kit.aifb.belt.db.QValue;

public class SRResultValue implements Comparable<SRResultValue> {
	private final EWAHCompressedBitmap[] props;
	private final EWAHCompressedBitmap[] types;
	private final int[] domains;
	private final int futureOffset;
	private final Action action;
	private final double q;
	
	private double similarity;

	public SRResultValue(EWAHCompressedBitmap[] props, EWAHCompressedBitmap[] types, int[] domains, int futureOffset, Action action,
			double q) {
		this.props = props;
		this.types = types;
		this.domains = domains;
		this.futureOffset = futureOffset;
		this.action = action;
		this.q = q;
	}

	public SRResultValue(SRKey key, SRQValue value) {
		props = value.getProps();
		types = value.getTypes();
		domains = value.getDomains();
		futureOffset = value.getFutureOffset();
		action = key.getPlainAction();
		q = value.getQ();
	}

	public EWAHCompressedBitmap[] getProps() {
		return props;
	}

	public EWAHCompressedBitmap[] getTypes() {
		return types;
	}
	
	public int[] getDomains() {
		return domains;
	}

	public int getFutureOffset() {
		return futureOffset;
	}

	public Action getAction() {
		return action;
	}

	public double getQ() {
		return q;
	}

	public int hashCode() {
		long q = Double.doubleToRawLongBits(this.q);

		return props.hashCode() ^ types.hashCode() ^ futureOffset ^ action.hashCode() ^ (int) q ^ (int) (q >> 32);
	}
	
	public boolean equals(Object o) {
		if (o instanceof SRResultValue) {
			SRResultValue r = (SRResultValue) o;
			
			return props.equals(r.props) && types.equals(r.types) && futureOffset == r.futureOffset && action.equals(r.action) && q == r.q;
		} else {
			return false;
		}
	}

	public int compareTo(SRResultValue o) {
		int hash = hashCode();
		int oHash = o.hashCode();
		
		if (hash < oHash) {
			return -1;
		} else if (hash == oHash) {
			return 0;
		} else {
			return 1;
		}
	}

	public double getSimilarity() {
		return similarity;
	}

	public void setSimilarity(double similarity) {
		this.similarity = similarity;
	}
}