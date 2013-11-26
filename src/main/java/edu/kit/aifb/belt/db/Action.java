package edu.kit.aifb.belt.db;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import edu.kit.aifb.belt.db.dict.StringDictionary;

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
	
	public byte[] getBytes(StringDictionary stringDict) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(out);
		
		try {
			data.writeLong(stringDict.getId(getUrl()));
			data.writeLong(stringDict.getId(getProperty()));
		} catch (IOException e) {
			// No actual IO involved.
			throw new RuntimeException(e);
		}
		
		return out.toByteArray();
	}
}