package edu.kit.aifb.belt.db;

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
	public void test() {

	}
}