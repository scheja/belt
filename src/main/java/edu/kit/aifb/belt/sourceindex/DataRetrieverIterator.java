package edu.kit.aifb.belt.sourceindex;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squin.lookup.deref.DataAnalyzer;
import org.squin.lookup.deref.URIDerefContext;
import org.squin.lookup.deref.jenaimpl.JenaIOBasedDerefContext;

import com.hp.hpl.jena.graph.Node;

public class DataRetrieverIterator extends DataAnalyzer.DataAnalyzingIterator {
	private URL currentURL = null;
	private URIDerefContext derefCxt;
	static List<String> urls = new ArrayList<String>();
	static Logger l = LoggerFactory.getLogger(DataRetrieverIterator.class);
	static int globalcounter = 0;
	private SourceIndexJenaImpl si;
	int counter = 0;
	
	public DataRetrieverIterator(Iterator<org.squin.dataset.Triple> input, URL src, URIDerefContext _derefCxt) {
		super(input);
		urls.add(src.toString());
		currentURL = src;
		derefCxt = _derefCxt; 
		counter = 0;
		si = new SourceIndexJenaImpl();
		System.out.println(globalcounter);
	}

	public static void addURL(String url) {
		urls.add(url);
		l.info("Found URL, which is not indexed: " + url);
	}

	public static List<String> getURLs() {
		return urls;
	}

	public static void print() {
		for (String url : urls) {
			System.out.println(url);
		}
	}

	public static void serialize(String path) {
		try {
			// use buffering
			OutputStream file = new FileOutputStream(path);
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				output.writeObject(urls);
			} finally {
				output.close();
			}
		} catch (IOException ex) {
			l.error("Cannot perform output.", ex);
		}
	}

	@SuppressWarnings("unchecked")
	public static void deserialize(String path) {
		try {
			// use buffering
			InputStream file = new FileInputStream(path);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer);
			try {
				try {
					urls = (List<String>) input.readObject();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			} finally {
				input.close();
			}
		} catch (IOException ex) {
			l.error("Cannot perform output.", ex);
		}
	}

	@Override
	protected void analyze(org.squin.dataset.Triple t) {
		Node g = Node.createURI(currentURL.toString());
		Node s = Node.createURI(((JenaIOBasedDerefContext) derefCxt).nodeDict.getNode(t.s).toString());
		Node p = Node.createURI(((JenaIOBasedDerefContext) derefCxt).nodeDict.getNode(t.p).toString());
		Node o = Node.createURI(((JenaIOBasedDerefContext) derefCxt).nodeDict.getNode(t.o).toString());
		if (g.toString() != "" && s.toString() != "" && p.toString() != "" && o.toString() != "") {
			si.addQuad(g, s, p, o);
		}
		counter++;
		globalcounter++;
	}
}
