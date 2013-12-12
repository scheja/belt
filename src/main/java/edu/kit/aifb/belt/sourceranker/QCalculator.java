package edu.kit.aifb.belt.sourceranker;

import java.util.List;

public interface QCalculator {
	public double calculateQ(List<SRResultValue> calculationMembers);
}