package edu.kit.aifb.belt.sourceranker;

import java.util.List;

public class AverageQCalculator implements QCalculator {
	public double calculateQ(List<SRResultValue> calculationMembers) {
		double q = 0;
		
		for (SRResultValue r : calculationMembers) {
			q += r.getQ();
		}
		
		if (q == 0) {
			return q;
		} else {
			return q / calculationMembers.size();
		}
	}
}