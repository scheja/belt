package edu.kit.aifb.belt.db.dict;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.squin.common.Statistics;
import org.squin.common.impl.StatisticsImpl;
import org.squin.dataset.jenacommon.NodeDictionary;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.hp.hpl.jena.graph.Node;

/**
 * A simple dictionary mapping from string to int. Thread-safe.
 * 
 * @author sibbo
 */
public class StringDictionary implements NodeDictionary {
	private Map<String, Integer> stringToIdMap = new HashMap<String, Integer>();
	private Map<Integer, String> idToStringMap = new HashMap<Integer, String>();
	private HashFunction murmur3 = Hashing.murmur3_128();
	
	private List<Integer> newIds = new ArrayList<Integer>();
	
	public synchronized void load(Iterator<Entry> iter) {
		while (iter.hasNext()) {
			Entry entry = iter.next();
			
			stringToIdMap.put(entry.getValue(), entry.getId());
			idToStringMap.put(entry.getId(), entry.getValue());
		}
	}

	public synchronized Set<Integer> getIds() {
		return idToStringMap.keySet();
	}
	
	public synchronized Collection<Integer> getNewIds() {
		return newIds;
	}
	
	public synchronized void clearNewIds() {
		newIds.clear();
	}

	public synchronized String getString(int id) {
		if (id == 0) {
			return null;
		}
		
		String result = idToStringMap.get(id);
		
		if (result == null) {
			throw new IllegalArgumentException("Unknown id: " + id);
		}
		
		return result;
	}
	
	/**
	 * Returns the id for the specified string. If no id exists, a new one is created and returned.
	 * @param value The string.
	 * @return A unique id for the string.
	 */
	public synchronized int getId(String value) {
		if (value == null) {
			return 0;
		}
		
		Integer result = stringToIdMap.get(value);
		
		if (result == null) {
			result = createId(value);
		}
		
		return result;
	}

	private synchronized int createId(String value) {
		try {
			int id = murmur3.hashBytes(value.getBytes("UTF-8")).asInt();
			
			while (idToStringMap.get(id) != null || id == 0) {
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

	public synchronized Node getNode(int id) {
		return Node.createURI(getString(id));
	}

	public synchronized int getId(Node n) {
		return getId(n.toString());
	}

	public synchronized int createId(Node n) {
		return getId(n.toString());
	}

	public synchronized Statistics getStatistics() {
		int size;
		synchronized ( this ) {
			size = idToStringMap.size();
		}

		StatisticsImpl.AttributeList statAttrs = new StatisticsImpl.AttributeList();
		statAttrs.add( "size", size );
		return new StatisticsImpl( statAttrs );
	}
	
	public class Entry {
		private int id;
		private String value;
		
		public Entry(int id, String value) {
			this.id = id;
			this.value = value;
		}

		public int getId() {
			return id;
		}

		public String getValue() {
			return value;
		}
	}
}