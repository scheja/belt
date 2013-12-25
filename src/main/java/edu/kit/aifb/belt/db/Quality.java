package edu.kit.aifb.belt.db;

public class Quality {
	private final double uniform;
	private final double normal;
	private final double exponential;

	public Quality(double uniform, double normal, double exponential) {
		this.uniform = uniform;
		this.normal = normal;
		this.exponential = exponential;
	}

	public double getUniform() {
		return uniform;
	}

	public double getNormal() {
		return normal;
	}

	public double getExponential() {
		return exponential;
	}
}