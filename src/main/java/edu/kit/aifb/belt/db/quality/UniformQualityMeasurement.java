package edu.kit.aifb.belt.db.quality;

public class UniformQualityMeasurement implements QualityMeasurement {
	private final double start;
	private final double end;

	public UniformQualityMeasurement() {
		this.start = 0;
		this.end = 1;
	}

	public UniformQualityMeasurement(double start, double end) {
		this.start = start;
		this.end = end;
	}

	public double getQuality(int id) {
		return Math.random() * (end - start) + start;
	}

	public double getMean() {
		return (end - start) / 2;
	}
}