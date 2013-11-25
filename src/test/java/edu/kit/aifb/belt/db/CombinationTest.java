package edu.kit.aifb.belt.db;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Multiset;

public class CombinationTest {
	@Ignore
	@Test
	public void testCombination() {
		String[] str = {"a", "b", "c", "d"};
		Combination<String> c = new Combination<String>(str);
		
		for (Multiset<String> result : c.combinations(2)) {
			for (String s : result) {
				System.out.print(s + " ");
			}
			
			System.out.println();
		}
	}
}