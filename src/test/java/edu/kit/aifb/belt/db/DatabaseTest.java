package edu.kit.aifb.belt.db;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

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
		QValue x = new QValue(new StateChain(new State("past1", "past2"), new State("pastx"), new State("pasty")),
				new Action("abc.de", "knows"), new StateChain(new State("future1", "future2"), new State("futurex"),
						new State("futurey")), 3);

		QValue y = new QValue(new StateChain(new State("pafsd", "pasfsd"), new State("pastx"), new State("pasty")),
				new Action("abc.de", "knows"), new StateChain(new State("future1", "future2"), new State("futurex"),
						new State("futurey")), 2);

		db.updateQ(x, y);

		x.setQ(-5);
		y.setQ(-5);

		db.getQ(x);
		db.getQ(y);

		assertTrue("Q must be 3 but was: " + x.getQ(), x.getQ() == 3);
		assertTrue("Q must be 2 but was: " + y.getQ(), y.getQ() == 2);
	}
}