/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.dataset.query;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents a triple pattern by identifiers for the RDF nodes as used by
 * the queried graph and by identifiers for the query variables as provided
 * to the query plan.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public class TriplePattern
{
	// members

	/** designates whether the identifier of the subject represents an RDF node or a query variable */
	final public boolean sIsVar;

	/** designates whether the identifier of the predicate represents an RDF node or a query variable */
	final public boolean pIsVar;

	/** designates whether the identifier of the object represents an RDF node or a query variable */
	final public boolean oIsVar;

	/** the identifier for the subject */
	final public int s;

	/** the identifier for the predicate */
	final public int p;

	/** the identifier for the object */
	final public int o;


	// initialization

	public TriplePattern ( boolean sIsVar, int s, boolean pIsVar, int p, boolean oIsVar, int o )
	{
		this.sIsVar = sIsVar;
		this.s = s;
		this.pIsVar = pIsVar;
		this.p = p;
		this.oIsVar = oIsVar;
		this.o = o;
	}
	
	public List<Integer> getVars() {
		List<Integer> al = new ArrayList<Integer>();
		if (this.sIsVar)
			al.add(this.s);		
		if (this.pIsVar)
			al.add(this.p);		
		if (this.oIsVar)
			al.add(this.o);		
		return al;
	}


	// redefinition of Object methods

	@Override
	public String toString ()
	{
		return "TriplePattern(" + (sIsVar?"v":"n") + String.valueOf(s) + "," + (pIsVar?"v":"n") + String.valueOf(p) + "," + (oIsVar?"v":"n") + String.valueOf(o) + ")";
	}

	public boolean containsVar(int varID) {
		return ((this.sIsVar && this.s == varID) || (this.pIsVar && this.p == varID) || (this.oIsVar && this.o == varID));
	}

}
