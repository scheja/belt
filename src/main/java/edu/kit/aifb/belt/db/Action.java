package edu.kit.aifb.belt.db;

public class Action {
	private static final String SEPARATOR = "§";

	private String url;
	private String property;

	public Action(String url, String property) {
		this.url = url;
		this.property = property;
	}

	public String toString() {
		return url + SEPARATOR + property;
	}

	public String getUrl() {
		return url;
	}

	public String getProperty() {
		return property;
	}
}