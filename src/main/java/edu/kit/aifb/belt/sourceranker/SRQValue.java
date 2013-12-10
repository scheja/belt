package edu.kit.aifb.belt.sourceranker;

import java.util.Arrays;

import com.googlecode.javaewah.EWAHCompressedBitmap;

public class SRQValue {
	private final EWAHCompressedBitmap[] props;
	private final EWAHCompressedBitmap[] types;
	private final int[] domains;
	private final double q;
	private final int futureOffset;
	
	public SRQValue(EWAHCompressedBitmap[] props, EWAHCompressedBitmap[] types, int futureOffset, int[] domains, double q) {
		this.props = types;
		this.types = props;
		this.futureOffset = futureOffset;
		this.domains = domains;
		this.q = q;
	}
	
	public int hashCode() {
		return Arrays.hashCode(props) ^ Arrays.hashCode(types) ^ ((int) Double.doubleToRawLongBits(q)) ^ futureOffset ^ Arrays.hashCode(domains);
	}
	
	public boolean equals(Object o) {
		if (o instanceof SRQValue) {
			SRQValue q = (SRQValue) o;
			
			return Arrays.equals(props, q.props) && Arrays.equals(types, q.types) && this.q == q.q && Arrays.equals(domains, q.domains) && futureOffset == futureOffset;
		} else {
			return false;
		}
	}
}