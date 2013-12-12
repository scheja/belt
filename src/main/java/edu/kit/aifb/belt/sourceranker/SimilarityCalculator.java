package edu.kit.aifb.belt.sourceranker;

import java.util.List;

import com.googlecode.javaewah.EWAHCompressedBitmap;

import edu.kit.aifb.belt.db.State;

public interface SimilarityCalculator {
	double calculateSimilarity(EWAHCompressedBitmap[] props1, EWAHCompressedBitmap[] types1, int futureOffset1,
			EWAHCompressedBitmap[] props2, EWAHCompressedBitmap[] types2, int futureOffset2);
}