package edu.kit.aifb.belt.db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.omg.CORBA.ORB;
import org.squin.dataset.query.TriplePattern;

import com.hp.hpl.jena.graph.Triple;

public class QueryGraph {
	private List<QueryNode> nodes;

	public QueryGraph() {
		nodes = new ArrayList<QueryNode>();
	}

	
	public List<QueryNode> getNodes() {
		return nodes;
	}


	public QueryNode add(Triple jenaTriple, TriplePattern triplepattern) {
		QueryNode node = new QueryNode(jenaTriple, triplepattern);
		nodes.add(node);
		
		int s = node.getTriplePattern().s;
		int o = node.getTriplePattern().o;

		for (QueryNode node1 : nodes) {
			if (node1.getTriplePattern().s == o && node.getTriplePattern().sIsVar) {
				node.addEdge(node1);
			}
			
			if (node1.getTriplePattern().o == s && node.getTriplePattern().oIsVar) {
				node1.addEdge(node);
			}				
		}
		
		return node;
	}

	public class QueryNode {
		private Triple jenaTriple;
		private TriplePattern triplepattern;
		private List<org.squin.dataset.Triple> bindings;
		private Triple binding;
		private QValue q;
		public final HashSet<Edge> inEdges;
		public final HashSet<Edge> outEdges;
		
		public QueryNode(Triple jenaTriple, TriplePattern triplepattern ) {
			this.jenaTriple = jenaTriple;
			this.triplepattern = triplepattern;
			this.bindings = new ArrayList<org.squin.dataset.Triple>();
			inEdges = new HashSet<Edge>();
			outEdges = new HashSet<Edge>();
		}

		public QueryNode addEdge(QueryNode node) {
			Edge e = new Edge(this, node);
			outEdges.add(e);
			node.inEdges.add(e);
			return this;
		}

		public Triple getJenaTriple() {
			return jenaTriple;
		}

		public void setJenaTriple(Triple jenaTriple) {
			this.jenaTriple = jenaTriple;
		}

		public TriplePattern getTriplePattern() {
			return triplepattern;
		}

		public void setTriplePattern(TriplePattern triplepattern) {
			this.triplepattern = triplepattern;
		}

		public List<org.squin.dataset.Triple> getBindings() {
			return bindings;
		}
		
		public void addBinding(org.squin.dataset.Triple t) {
			// prevent double insertion
			if (!this.getBindings().contains(t)) {
				this.getBindings().add(t);	
			}				
		}

		public void setBindings(List<org.squin.dataset.Triple> bindings) {
			this.bindings = bindings;
		}

		public HashSet<Edge> getInEdges() {
			return inEdges;
		}

		public HashSet<Edge> getOutEdges() {
			return outEdges;
		}
		
		public String toString() {
			return jenaTriple.toString();
		}
		
		
	}
	
	  public static class Edge{
		    public final QueryNode from;
		    public final QueryNode to;
		    public Edge(QueryNode from, QueryNode to) {
		      this.from = from;
		      this.to = to;
		    }
		    @Override
		    public boolean equals(Object obj) {
		      Edge e = (Edge)obj;
		      return e.from == from && e.to == to;
		    }
		  }

}
