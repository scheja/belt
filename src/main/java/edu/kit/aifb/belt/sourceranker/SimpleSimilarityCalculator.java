package edu.kit.aifb.belt.sourceranker;

import com.googlecode.javaewah.EWAHCompressedBitmap;

public class SimpleSimilarityCalculator extends SimilarityCalculator {
	public double calculateSimilarity(EWAHCompressedBitmap[] props1, EWAHCompressedBitmap[] types1, int futureOffset1,
			int[] domains1, EWAHCompressedBitmap[] props2, EWAHCompressedBitmap[] types2, int futureOffset2,
			int[] domains2) {
		final int length = getLength(props1.length, futureOffset1, props2.length, futureOffset2);
		final int overlappingStart = getOverlappingStart(futureOffset1, futureOffset2);
		final int overlappingEnd = getOverlappingEnd(props1.length, futureOffset1, props2.length, futureOffset2);

		double similarity = 0;

		for (int i = overlappingStart; i <= overlappingEnd; i++) {
			EWAHCompressedBitmap propA = get(i, props1, futureOffset1);
			EWAHCompressedBitmap propB = get(i, props2, futureOffset2);
			EWAHCompressedBitmap typeA = get(i, types1, futureOffset1);
			EWAHCompressedBitmap typeB = get(i, types2, futureOffset2);
			int domA = get(i, domains1, futureOffset1);
			int domB = get(i, domains2, futureOffset2);

			double propSimilarity = propA.andCardinality(propB);
			propSimilarity /= propA.cardinality() + propB.cardinality();

			double typeSimilarity = typeA.andCardinality(typeB);
			typeSimilarity /= typeA.cardinality() + typeB.cardinality();

			double domainSimilarity = domA == domB ? 1 / (propA.cardinality() + propB.cardinality()
					+ typeA.cardinality() + typeB.cardinality() + 1) : 0;

			similarity += propSimilarity;
			similarity += typeSimilarity;
			similarity += domainSimilarity;
		}

		similarity /= length;

		return similarity;
	}
}