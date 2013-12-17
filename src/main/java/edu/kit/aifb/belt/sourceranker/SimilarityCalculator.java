package edu.kit.aifb.belt.sourceranker;

import com.googlecode.javaewah.EWAHCompressedBitmap;

public abstract class SimilarityCalculator {
	public abstract double calculateSimilarity(EWAHCompressedBitmap[] props1, EWAHCompressedBitmap[] types1,
			int futureOffset1, EWAHCompressedBitmap[] props2, EWAHCompressedBitmap[] types2, int futureOffset2);

	protected int getLength(int length1, int futureOffset1, int length2, int futureOffset2) {
		return Math.max(length1 - futureOffset1, length2 - futureOffset2) + Math.max(futureOffset1, futureOffset2);
	}

	protected int getOverlappingStart(int futureOffset1, int futureOffset2) {
		return 1 - Math.min(futureOffset1, futureOffset2);
	}
	
	protected int getOverlappingEnd(int length1, int futureOffset1, int length2, int futureOffset2) {
		 return Math.min(length1 - futureOffset1, length2 - futureOffset2);
	}
	
	protected EWAHCompressedBitmap get(int index, EWAHCompressedBitmap[] array, int futureOffset) {
		return array[index + futureOffset - 1];
	}
}