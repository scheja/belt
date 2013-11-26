package edu.kit.aifb.belt.db;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class Combination<T> {
	private T[] source;

	public Combination(T[] source) {
		this.source = source;
	}

	public List<Multiset<T>> combinations(int size) {
		BigInteger resultSize = getResultSize(size);

		if (resultSize.compareTo(new BigInteger("" + Integer.MAX_VALUE)) > 0) {
			throw new IllegalArgumentException("Combination possibilities to large: " + resultSize);
		}
		
		List<Multiset<T>> result = new ArrayList<Multiset<T>>(resultSize.intValue());
		Deque<T> candidate = new ArrayDeque<T>(size);
		
		createCombinations(result, candidate, 0, size);
		return result;
	}

	private void createCombinations(List<Multiset<T>> result, Deque<T> candidate, int offset, int size) {
		if (candidate.size() == size) {
			Multiset<T> set = HashMultiset.create();
			set.addAll(candidate);
			result.add(set);
		} else {
			for (int i = offset; i <= source.length - size + candidate.size(); i++) {
				candidate.push(source[i]);
				createCombinations(result, candidate, i + 1, size);
				candidate.pop();
			}
		}
	}

	private BigInteger faculty(int x) {
		BigInteger result = BigInteger.ONE;

		for (int i = 2; i <= x; i++) {
			result = result.multiply(new BigInteger("" + i));
		}

		return result;
	}
	
	public BigInteger getResultSize(int size) {
		BigInteger resultSize = faculty(source.length);
		resultSize = resultSize.divide(faculty(size));
		return resultSize.divide(faculty(source.length - size));
	}
}