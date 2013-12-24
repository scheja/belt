package edu.kit.aifb.belt.querypool;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.google.common.base.Joiner;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.sparql.core.TriplePath;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;
import com.hp.hpl.jena.sparql.syntax.ElementVisitorBase;
import com.hp.hpl.jena.sparql.syntax.ElementWalker;

public class Importer {
	
	private static ArrayList<String> prefixes = new ArrayList<String>();
	private static int queriesInspected = 0;	
	private static int queriesImported = 0;
	private static int bgpcounter = 0;

	public static void main(String[] args) {
		String s = "/Users/janscheurenbrand/Code/usewod2013_dataset/usewod2013_dataset/data/CLF-server-logs/dbpedia/jantest/";
		File path = new File(s);
		preparePrefixlist();
		readDir(path);
	}
	
	private static void preparePrefixlist() {
		prefixes.add("PREFIX dbpprop: 	<http://dbpedia.org/property/>");
		prefixes.add("PREFIX p: 			<http://dbpedia.org/property/>");
		prefixes.add("PREFIX dbpedia: 	<http://dbpedia.org/resource/>");
		prefixes.add("PREFIX dbpedia2: 	<http://dbpedia.org/resource/>");
		prefixes.add("PREFIX dbpedia-owl:<http://dbpedia.org/ontology/>");
		prefixes.add("PREFIX category:	<http://dbpedia.org/resource/Category:>");
		prefixes.add("PREFIX rdfs: 		<http://www.w3.org/2000/01/rdf-schema#>");
		prefixes.add("PREFIX skos: 		<http://www.w3.org/2004/02/skos/core#>");
		prefixes.add("PREFIX foaf: 		<http://xmlns.com/foaf/0.1/>");
		prefixes.add("PREFIX doap: 		<http://usefulinc.com/ns/doap#>");
		prefixes.add("PREFIX wgs84_pos: 		<http://www.w3.org/2003/01/geo/wgs84_pos#>");
		prefixes.add("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>");
		prefixes.add("PREFIX owl: <http://www.w3.org/2002/07/owl#>"); 
		prefixes.add("PREFIX geo: 		<http://www.georss.org/georss/>");
	}
	
	public static void readDir(File path) {
	    File[] files = path.listFiles();
	    for (File file : files) {
	        if (file.isDirectory()) {
	            System.out.println("Directory: " + file.getName());
	            readDir(file);
	        } else {
	            System.out.println("File: " + file.getName());
	            String extension = "";
	            int i = file.getName().lastIndexOf('.');
	            if (i > 0) {
	                extension = file.getName().substring(i+1);
	            }
	            
	            if (extension.equals("bz2")) {
		            System.out.println("Compressed File. Needs some extraction");
		            readCompressedFile(file);
	            }
	            
	            if (extension.equals("log")) {
		            System.out.println("Log File. Let's work with it.");
	            }
	        }
	    }
		
	}
	
	public static void readCompressedFile(File path) {		
		try {
		    FileInputStream fin = new FileInputStream(path);
		    BufferedInputStream bis = new BufferedInputStream(fin);
		    CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
		    BufferedReader br = new BufferedReader(new InputStreamReader(input));		    
		    readLineByLine(br);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public static void readLineByLine(BufferedReader br) throws IOException {
	  String strLine;
	  while((strLine = br.readLine())!= null) {
		  parse(strLine,0);
	  }	
	}
	
	public static void parse(String line, int retry) {
		queriesInspected++;
		String[] parts = line.split(" ");
		try {
			String clean = URLDecoder.decode(parts[6],"utf-8");
			clean = clean.substring(clean.indexOf("SELECT") , clean.lastIndexOf("}")+1);
			Joiner joiner = Joiner.on("\n");
			clean = joiner.join(prefixes) + clean;
			Query query = QueryFactory.create(clean);
			
			if (isForLinkTraversalQuery(query)) {
				saveQuery(query);
			} else {
				System.out.print("1");
			}
		} catch (QueryParseException e) {
			System.out.print("2");
			// prevent loop
			if (retry > 3)
				return;
			if (e.getMessage().contains("Unresolved prefixed name")) {
				String prefixed = e.getMessage().substring(e.getMessage().indexOf("Unresolved prefixed name: ")+26, e.getMessage().length());
				String prefix = prefixed.substring(0, prefixed.indexOf(":"));
				if (!prefix.equals("")) {
					try {
						URL prefixcc = new URL("http://prefix.cc/" + prefix + ".file.sparql");
				        URLConnection prefixccconn = prefixcc.openConnection();
				        BufferedReader in = new BufferedReader( new InputStreamReader( prefixccconn.getInputStream()));
				        // we just need the first line
				        String inputLine = in.readLine();
				        in.close();
				        prefixes.add(inputLine);
				        System.out.print("3");
				        parse(line,retry+1);				       				        
					} catch (Exception e1) {
						// It's ok...
					}					
				}


			}			
		} catch (StringIndexOutOfBoundsException e) {
			System.out.print("5");
			
		} catch (Exception e) {
			System.out.print("6");
			e.printStackTrace();
		}
		if (queriesInspected % 100 == 0) {
			System.out.println("");
		}
		if (queriesInspected % 1000 == 0) {
		    System.out.println(queriesInspected + "/" + queriesImported);
		}
		
	}
	
	private static void saveQuery(Query query) {
		bgpcounter = 0;
    	// This will walk through all parts of the query
        ElementWalker.walk(query.getQueryPattern(),
            // For each element...
            new ElementVisitorBase() {
                // ...when it's a block of triples...
                public void visit(ElementPathBlock el) {
                    // ...go through all the triples...
                    Iterator<TriplePath> triples = el.patternElts();
                    while (triples.hasNext()) {
                    	TriplePath tp = triples.next();
                    	bgpcounter++;                    	
                    }
                }
            }
        );
      
        if ((bgpcounter > 5)) {
        	
        	if (bgpcounter < 11) {
                System.out.print(".");
        		queriesImported++;	
        	} else {
                System.out.print("8");
        	}       	

        } else {
            System.out.print("7");
        }

	}

	public static boolean isForLinkTraversalQuery(Query query) {
        final DirectedGraph<String, DefaultEdge> g = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
        
    	// This will walk through all parts of the query
        ElementWalker.walk(query.getQueryPattern(),
            // For each element...
            new ElementVisitorBase() {
                // ...when it's a block of triples...
                public void visit(ElementPathBlock el) {
                    // ...go through all the triples...
                    Iterator<TriplePath> triples = el.patternElts();
                    while (triples.hasNext()) {
                    	TriplePath t = triples.next();
            			g.addVertex(t.getSubject().toString());
            			g.addVertex(t.getObject().toString());
            			g.addEdge(t.getSubject().toString(), t.getObject().toString());
                    }
                }
            }
        );

		ConnectivityInspector<String, DefaultEdge> ci = new ConnectivityInspector<String, DefaultEdge>(g); 
		return ci.isGraphConnected();
	}

}
