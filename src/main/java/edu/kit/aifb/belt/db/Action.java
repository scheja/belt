package edu.kit.aifb.belt.db;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import edu.kit.aifb.belt.db.dict.StringDictionary;

public class Action {
	private static final String SEPARATOR = "§";

	private int url;
	private int property;

	public Action(String url, String property, StringDictionary dict) {
		this.url = dict.getId(url);
		this.property = dict.getId(property);
	}

	public String toString() {
		return url + SEPARATOR + property;
	}

	public String getUrl(StringDictionary dict) {
		return dict.getString(url);
	}

	public String getProperty(StringDictionary dict) {
		return dict.getString(property);
	}
	
	public byte[] getBytes(StringDictionary stringDict) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(out);
		
		try {
			data.writeLong(url);
			data.writeLong(property);
		} catch (IOException e) {
			// No actual IO involved.
			throw new RuntimeException(e);
		}
		
		return out.toByteArray();
	}
}