package edu.kit.aifb.belt.sourceindex;

import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Quad;

/**
 * @author janscheurenbrand
 * Interface for the Source Index, the component which simulates the WWW
 * and caches real data locally.
 *
 */
public interface SourceIndex {
	
	/**
	 * Adds a Quad to the Source Index
	 * @param g The URI of the original source containing the Quad
	 * @param s Subject
	 * @param p Predicate
	 * @param o Object
	 */
	public void addQuad(Node g, Node s, Node p, Node o);
	
	
	/**
	 * Gets all the Quads which were found at the original source
	 * @param uri The URI to search for
	 * @return an Iterator over all found Quads
	 */
	public Iterator<Quad> findAllByURI(String uri);
	
	/**
	 * Updates the URIs of Quads in the Source Index
	 * E.g. after a redirect
	 * @param from
	 * @param to
	 */
	public void updateURIs(String from, String to);

}
