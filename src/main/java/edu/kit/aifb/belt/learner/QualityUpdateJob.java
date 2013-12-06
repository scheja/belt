package edu.kit.aifb.belt.learner;

public class QualityUpdateJob extends Job {
	private String url;

	public QualityUpdateJob(String url) {
		this.url = url;
	}

	public String getURL() {
		return url;
	}

}