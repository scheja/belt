package edu.kit.aifb.belt.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Quad;

import edu.kit.aifb.belt.db.quality.QualityMeasurement;
import edu.kit.aifb.belt.sourceindex.SourceIndex;

public class DatabaseTest {
	private static Database db;

	@BeforeClass
	public static void setUpBeforeClass() {
		db = new Database();
		db.connect();
	}

	@Test
	public void testInsert() {
		Set<String> aSet = new HashSet<String>();
		aSet.add("a");
		Set<String> pasfsdSet = new HashSet<String>();
		pasfsdSet.add("pasfsd");

		QValue x = new QValue(new StateChain(new State("a", aSet, db.getDictionary(), "past1", "past2"), new State("a",
				aSet, db.getDictionary(), "pastx"), new State("a", aSet, db.getDictionary(), "pasty")), new Action(
				"abc.de", "knows", db.getDictionary()), new StateChain(new State("a", aSet, db.getDictionary(),
				"future1", "future2"), new State("a", aSet, db.getDictionary(), "futurex"), new State("a", aSet,
				db.getDictionary(), "futurey")), 3);

		QValue y = new QValue(new StateChain(new State("pafsd", pasfsdSet, db.getDictionary()), new State("a", aSet,
				db.getDictionary(), "pastx"), new State("a", aSet, db.getDictionary(), "pasty")), new Action("abc.de",
				"knows", db.getDictionary()), new StateChain(new State("a", aSet, db.getDictionary(), "future1",
				"future2"), new State("a", aSet, db.getDictionary(), "futurex"), new State("a", aSet,
				db.getDictionary(), "futurey")), 2);

		db.updateQ(x, y);

		x.setQ(-5);
		y.setQ(-5);

		db.getQ(x);
		db.getQ(y);

		assertTrue("Q must be 3 but was: " + x.getQ(), x.getQ() == 3);
		assertTrue("Q must be 2 but was: " + y.getQ(), y.getQ() == 2);
	}

	@Test
	public void testStateSorting() {
		Set<String> aSet = new HashSet<String>();
		aSet.add("a");
		Set<String> pasfsdSet = new HashSet<String>();
		pasfsdSet.add("pasfsd");

		QValue x = new QValue(new StateChain(new State("a", aSet, db.getDictionary(), "past1", "past2"), new State("a",
				aSet, db.getDictionary(), "pastx"), new State("a", aSet, db.getDictionary(), "pasty")), new Action(
				"abc.de", "knows", db.getDictionary()), new StateChain(new State("a", aSet, db.getDictionary(),
				"future1", "future2"), new State("a", aSet, db.getDictionary(), "futurex"), new State("a", aSet,
				db.getDictionary(), "futurey")), 3);

		QValue y = new QValue(new StateChain(new State("a", aSet, db.getDictionary(), "past2", "past1"), new State("a",
				aSet, db.getDictionary(), "pastx"), new State("a", aSet, db.getDictionary(), "pasty")), new Action(
				"abc.de", "knows", db.getDictionary()), new StateChain(new State("a", aSet, db.getDictionary(),
				"future2", "future1"), new State("a", aSet, db.getDictionary(), "futurex"), new State("a", aSet,
				db.getDictionary(), "futurey")), 2);

		db.updateQ(x, y);

		x.setQ(-5);
		y.setQ(-5);

		db.getQ(x);
		db.getQ(y);

		assertTrue("Q must be 3 but was: " + x.getQ(), x.getQ() == 2);
		assertTrue("Q must be 2 but was: " + y.getQ(), y.getQ() == 2);
	}

	@Test
	public void testSourceIndex() {
		SourceIndex s = db;

		Quad q = new Quad(Node.createURI("c"), Node.createURI("s"), Node.createURI("p"), Node.createURI("o"));
		Quad q2 = new Quad(Node.createURI("d"), Node.createURI("s"), Node.createURI("p"), Node.createURI("o"));

		db.deleteQuad(q);
		db.deleteQuad(q2);

		assertFalse("Could not delete Quad.", s.findAllByURI("c").hasNext());
		assertFalse("Could not delete Quad.", s.findAllByURI("d").hasNext());

		s.addQuad(q.getGraph(), q.getSubject(), q.getPredicate(), q.getObject());

		int size = iteratorSize(s.findAllByURI("c"));
		assertEquals("Wrong number of quads inserted.", 1, size);

		// Second time, to assure we don't insert two times.
		s.addQuad(q.getGraph(), q.getSubject(), q.getPredicate(), q.getObject());

		size = iteratorSize(s.findAllByURI("c"));
		assertEquals("Wrong number of quads inserted.", 1, size);

		s.addRedirect("d", "c");
		s.handleRedirections();
		size = iteratorSize(s.findAllByURI("d"));
		assertEquals("Wrong number of quads found with new uri.", 1, size);
	}

//	@Test
//	public void testQuality() {
//		final double quality = Math.random() + 1;
//
//		db.setQualityMeasurement(new QualityMeasurement() {
//			public double getQuality(int id) {
//				return quality;
//			}
//			
//			public double getMean() {
//				return 1.5;
//			}
//		});
//		
//		Node context = Node.createURI("quality.test");
//		
//		db.deleteQuality(context);
//		db.deleteQuad(new Quad(context, Node.createURI("s"), Node.createURI("p"), Node.createURI("o")));
//		db.addQuad(context, Node.createURI("s"), Node.createURI("p"), Node.createURI("o"));
//		
//		assertEquals("The quality was not inserted correctly.", quality, db.getQuality(context), 1e-10);
//		
//		db.incrementQuality(context.toString(), 0.33);
//		
//		assertEquals("The quality was no incremented correctly.", quality + 0.33, db.getQuality(context), 1e-10);
//	}
	
	@Test
	public void testListQ() {
		Set<String> aSet = new HashSet<String>();
		aSet.add("a");
		Set<String> pasfsdSet = new HashSet<String>();
		pasfsdSet.add("pasfsd");

		QValue x = new QValue(new StateChain(new State("a", aSet, db.getDictionary(), "past1", "past2"), new State("a",
				aSet, db.getDictionary(), "pastx"), new State("a", aSet, db.getDictionary(), "pasty")), new Action(
				"abc.de", "knows", db.getDictionary()), new StateChain(new State("a", aSet, db.getDictionary(),
				"future1", "future2"), new State("a", aSet, db.getDictionary(), "futurex"), new State("a", aSet,
				db.getDictionary(), "futurey")), 3);

		QValue y = new QValue(new StateChain(new State("y", aSet, db.getDictionary(), "pafcsast2", "pasteca1"), new State("arfeca",
				aSet, db.getDictionary(), "pastx"), new State("a", aSet, db.getDictionary(), "pasty")), new Action(
				"abc.de", "knows", db.getDictionary()), new StateChain(new State("a", aSet, db.getDictionary(),
				"future2", "futurfxdsae1"), new State("a", aSet, db.getDictionary(), "futurexfsda"), new State("a", aSet,
				db.getDictionary(), "futurey")), 2);
		
		db.updateQ(x);
		db.updateQ(y);
		
		int xCount = 0;
		int yCount = 0;
		
		
		for (Iterator<QValue> i = db.listAllQs(); i.hasNext();) {
			 QValue q = i.next();
			 
			 if (q.equals(x)) {
				 xCount++;
			 }
			 
			 if (q.equals(y)) {
				 yCount++;
			 }
		}
		
		assertEquals("Wrong amount of x found", 1, xCount);
		assertEquals("Wrong amount of y found", 1, yCount);
	}
	
	@Test
	public void testQueryTable() {
		String query = "BlaBlub";
		List<String> results = new ArrayList<String>();
		results.add("ResultHighRank");
		results.add("ResultLowRank");
		
		db.insertQueryResults(query, results.iterator());
		List<String> dbResult = db.getQueryResults(query);
		
		assertTrue("Wrong results. Should: " + Arrays.toString(results.toArray()) + ", Is: " + Arrays.toString(dbResult.toArray()), Arrays.equals(results.toArray(), dbResult.toArray()));
		
		db.deleteQueryResults(query);
	}

	private int iteratorSize(@SuppressWarnings("rawtypes") Iterator i) {
		int size = 0;

		while (i.hasNext()) {
			i.next();
			size++;
		}

		return size;
	}
	
	

	@AfterClass
	public static void tearDownAfterClass() {
		db.close();
	}
}