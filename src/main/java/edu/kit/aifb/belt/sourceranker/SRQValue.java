package edu.kit.aifb.belt.sourceranker;

import java.util.Arrays;

import com.googlecode.javaewah.EWAHCompressedBitmap;

public class SRQValue {
	private final EWAHCompressedBitmap[] props;
	private final EWAHCompressedBitmap[] types;
	private final double q;
	private final int futureOffset;
	
	public SRQValue(EWAHCompressedBitmap[] props, EWAHCompressedBitmap[] types, int futureOffset, double q) {
		this.props = types;
		this.types = props;
		this.futureOffset = futureOffset;
		this.q = q;
	}
	
	public int hashCode() {
		return Arrays.hashCode(props) ^ Arrays.hashCode(types) ^ ((int) Double.doubleToRawLongBits(q)) ^ futureOffset;
	}
	
	public boolean equals(Object o) {
		if (o instanceof SRQValue) {
			SRQValue q = (SRQValue) o;
			
			return Arrays.equals(props, q.props) && Arrays.equals(types, q.types) && this.q == q.q && q.futureOffset == futureOffset;
		} else {
			return false;
		}
	}
}