package edu.kit.aifb.belt.db.quality;

import java.util.Random;

public class NormalQualityMeasurement implements QualityMeasurement {
	private final double mean;
	private final double deviation;
	private final Random r = new Random();
	
	public NormalQualityMeasurement(double mean, double deviation) {
		this.mean = mean;
		this.deviation = deviation;
	}

	public double getQuality(int id) {
		return r.nextGaussian() * deviation + mean;
	}

	public double getMean() {
		return mean;
	}
}