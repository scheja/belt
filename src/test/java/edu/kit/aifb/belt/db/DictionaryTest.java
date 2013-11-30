package edu.kit.aifb.belt.db;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.kit.aifb.belt.db.dict.StringDictionary;

public class DictionaryTest {
	private static Database db;

	@BeforeClass
	public static void setUpBeforeClass() {
		db = new Database();
		db.connect();
	}
	
	@Test
	public void testDictionary() {
		StringDictionary dict = db.getDictionary();
		int id = dict.getId("TestValue!");
		
		db.close();
		db.connect();
		dict = db.getDictionary();
		
		assertEquals("Ids didn't match", id, dict.getId("TestValue!"));
		assertEquals("Strings didn't match", "TestValue!", dict.getString(id));
	}
	
	@AfterClass
	public static void tearDownAfterClass() {
		db.close();
	}
}