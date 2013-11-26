package edu.kit.aifb.belt.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Quad;

import edu.kit.aifb.belt.sourceindex.SourceIndex;

public class DatabaseTest {
	private static Database db;

	@BeforeClass
	public static void setUpBeforeClass() {
		db = new Database("janscheurenbrand.de/belt");
		db.connect();
	}

	@Test
	public void testInsert() {
		QValue x = new QValue(new StateChain(new State("a", "a", db.getDictionary(), "past1", "past2"), new State("a",
				"a", db.getDictionary(), "pastx"), new State("a", "a", db.getDictionary(), "pasty")), new Action(
				"abc.de", "knows", db.getDictionary()), new StateChain(new State("a", "a", db.getDictionary(),
				"future1", "future2"), new State("a", "a", db.getDictionary(), "futurex"), new State("a", "a",
				db.getDictionary(), "futurey")), 3);

		QValue y = new QValue(new StateChain(new State("pafsd", "pasfsd", db.getDictionary()), new State("a", "a",
				db.getDictionary(), "pastx"), new State("a", "a", db.getDictionary(), "pasty")), new Action("abc.de",
				"knows", db.getDictionary()), new StateChain(new State("a", "a", db.getDictionary(), "future1",
				"future2"), new State("a", "a", db.getDictionary(), "futurex"), new State("a", "a", db.getDictionary(),
				"futurey")), 2);

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
		QValue x = new QValue(new StateChain(new State("a", "a", db.getDictionary(), "past1", "past2"), new State("a",
				"a", db.getDictionary(), "pastx"), new State("a", "a", db.getDictionary(), "pasty")), new Action(
				"abc.de", "knows", db.getDictionary()), new StateChain(new State("a", "a", db.getDictionary(),
				"future1", "future2"), new State("a", "a", db.getDictionary(), "futurex"), new State("a", "a",
				db.getDictionary(), "futurey")), 3);

		QValue y = new QValue(new StateChain(new State("a", "a", db.getDictionary(), "past2", "past1"), new State("a",
				"a", db.getDictionary(), "pastx"), new State("a", "a", db.getDictionary(), "pasty")), new Action(
				"abc.de", "knows", db.getDictionary()), new StateChain(new State("a", "a", db.getDictionary(),
				"future2", "future1"), new State("a", "a", db.getDictionary(), "futurex"), new State("a", "a",
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
		
		s.updateURIs("c", "d");
		size = iteratorSize(s.findAllByURI("d"));
		assertEquals("Wrong number of quads found with new uri.", 1, size);
	}

	private int iteratorSize(@SuppressWarnings("rawtypes") Iterator i) {
		int size = 0;

		while (i.hasNext()) {
			i.next();
			size++;
		}

		return size;
	}
}