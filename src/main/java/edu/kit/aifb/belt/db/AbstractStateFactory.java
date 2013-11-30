package edu.kit.aifb.belt.db;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.hp.hpl.jena.sparql.core.Quad;

import edu.kit.aifb.belt.db.dict.StringDictionary;
import edu.kit.aifb.belt.sourceindex.SourceIndex;

public abstract class AbstractStateFactory {
	public abstract List<State> createState(String domain, Set<String> type, Multiset<String> properties,
			StringDictionary dict);

	public List<State> createState(String domain, Set<String> type, StringDictionary dict, String... properties) {
		Multiset<String> set = HashMultiset.create();
		set.addAll(Arrays.asList(properties));

		return createState(domain, type, set, dict);
	}

	public List<State> createState(Multiset<String> properties, StringDictionary dict) {
		return createState(null, null, properties, dict);
	}

	public List<State> createState(StringDictionary dict, String... properties) {
		return createState(null, null, dict, properties);
	}

	public List<State> createState(String url, Database source) {
		Iterator<Quad> quads = source.findAllByURI(url);
		Multiset<String> properties = HashMultiset.create();
		Set<String> type = new HashSet<String>();
		
		while (quads.hasNext()) {
			Quad quad = quads.next();
			
			if (quad.getPredicate().equals("rdf:type") || quad.getPredicate().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
				type.add(quad.getPredicate().toString());
			}
		}
		
		URL u = null;
		
		try {
			u = new URL(url);
		} catch (MalformedURLException e) {
			Logger.getLogger(getClass()).log(Level.WARN, "Malformed URL", e);
		}
		
		return createState(u.getHost(), type, properties, source.getDictionary());
	}
	
	public List<State> createStateWithoutDomain(String url, Database source) {
		Iterator<Quad> quads = source.findAllByURI(url);
		Multiset<String> properties = HashMultiset.create();
		Set<String> type = new HashSet<String>();
		
		while (quads.hasNext()) {
			Quad quad = quads.next();
			
			if (quad.getPredicate().equals("rdf:type") || quad.getPredicate().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
				type.add(quad.getPredicate().toString());
			}
		}
		
		return createState(null, type, properties, source.getDictionary());
	}
}