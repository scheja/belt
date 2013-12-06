package edu.kit.aifb.belt.db;

public class EquallyDistributedRandomQualityMeasurement implements QualityMeasurement {
	public double getQuality(int id) {
		return Math.random();
	}
}