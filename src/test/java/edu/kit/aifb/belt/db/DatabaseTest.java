package edu.kit.aifb.belt.db;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

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
}