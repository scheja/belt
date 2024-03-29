package edu.kit.aifb.belt.db;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import edu.kit.aifb.belt.db.dict.StringDictionary;

public class Action {
	private int domain;
	private int property;

	public Action(String domain, String property, StringDictionary dict) {
		this.domain = dict.getId(domain);
		this.property = dict.getId(property);
	}
	
	// In LearnPartialChainIterator#learn the ID of the property is already present, so let's use it
	public Action(String domain, int property_id, StringDictionary dict) {
		this.domain = dict.getId(domain);
		this.property = property_id;
	}
	
	public Action(int domain, int property) {
		this.domain = domain;
		this.property = property;
	}

	public Action(InputStream in) {
		DataInputStream data = new DataInputStream(in);
		
		try {
			domain = data.readInt();
			property = data.readInt();
		} catch (IOException e) {
			// No actual IO involved.
			throw new RuntimeException(e);
		}
	}

	public String toString() {
		return domain + ", " + property;
	}

	public String getDomain(StringDictionary dict) {
		return dict.getString(domain);
	}

	public String getProperty(StringDictionary dict) {
		return dict.getString(property);
	}
	
	public byte[] getBytes(StringDictionary stringDict) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(out);
		
		try {
			data.writeInt(domain);
			data.writeInt(property);
		} catch (IOException e) {
			// No actual IO involved.
			throw new RuntimeException(e);
		}
		
		return out.toByteArray();
	}
	
	public int hashCode() {
		return domain ^ property;
	}
	
	public boolean equals(Object o) {
		if (o instanceof Action) {
			 Action a = (Action) o;
			
			return a.domain == domain && a.property == property;
		} else {
			return false;
		}
	}

	public int getProperty() {
		return property;
	}
	
	public int getDomain() {
		return domain;
	}
}
