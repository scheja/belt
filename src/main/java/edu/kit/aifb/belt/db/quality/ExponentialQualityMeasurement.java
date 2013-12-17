package edu.kit.aifb.belt.db.quality;

public class ExponentialQualityMeasurement implements QualityMeasurement {
	private final double rate;

	public ExponentialQualityMeasurement(double rate) {
		this.rate = rate;
	}

	public double getQuality(int id) {
		return Math.log(1 - Math.random()) / -rate;
	}

	public double getMean() {
		return 1 / rate;
	}
}
