package edu.kit.aifb.belt.db.dict;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class StringDictionary {
	private Map<String, Long> stringToIdMap = new HashMap<String, Long>();
	private Map<Long, String> idToStringMap = new HashMap<Long, String>();
	private HashFunction murmur3 = Hashing.murmur3_128();
	
	private List<Long> newIds = new ArrayList<Long>();
	
	public void load(Iterator<Entry> iter) {
		while (iter.hasNext()) {
			Entry entry = iter.next();
			
			stringToIdMap.put(entry.getValue(), entry.getId());
			idToStringMap.put(entry.getId(), entry.getValue());
		}
	}
	
	public class Entry {
		private long id;
		private String value;
		
		public Entry(long id, String value) {
			this.id = id;
			this.value = value;
		}

		public long getId() {
			return id;
		}

		public String getValue() {
			return value;
		}
	}

	public Set<Long> getIds() {
		return idToStringMap.keySet();
	}
	
	public Collection<Long> getNewIds() {
		return newIds;
	}
	
	public void clearNewIds() {
		newIds.clear();
	}

	public String getString(long id) {
		return idToStringMap.get(id);
	}
	
	/**
	 * Returns the id for the specified string. If no id exists, a new one is created and returned.
	 * @param value The string.
	 * @return A unique id for the string.
	 */
	public long getId(String value) {
		Long result = stringToIdMap.get(value);
		
		if (result == null) {
			result = createId(value);
		}
		
		return result;
	}

	private Long createId(String value) {
		try {
			long id = murmur3.hashBytes(value.getBytes("UTF-8")).asLong();
			
			while (idToStringMap.get(id) != null) {
				id++;
			}
			
			idToStringMap.put(id, value);
			stringToIdMap.put(value, id);
			
			newIds.add(id);
			return id;
		} catch (UnsupportedEncodingException e) {
			// Everyone supports UTF-8
			throw new RuntimeException(e);
		}
	}
}