package edu.kit.aifb.belt.db.metrics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Test;

import edu.kit.aifb.belt.metrics.Metrics;
import edu.kit.aifb.belt.metrics.Timer;

public class MetricsTest {
	@Test
	public void testAttributesTable() {
		Metrics m = Metrics.getInstance();

		m.setParameter("Test", 5);
		m.close();
	}

	@Test
	public void testSaveTimer() {
		Metrics m = Metrics.getInstance();

		Timer timer = new Timer();
		timer.start();
		Thread.yield();
		timer.pause();
		timer.unpause();
		Thread.yield();
		timer.pause();
		timer.unpause();
		Thread.yield();
		timer.stop();

		m.saveTimer(
				"Veryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryverylongstring",
				timer);
		m.close();
	}
	
	@Test
	public void testCustomMetric() {
		Metrics m = Metrics.getInstance();
		
		m.createMetric("TestMetric");
		m.insertValue("TestMetric", 4);
		m.insertValue("TestMetric", 3);
		m.insertValue("TestMetric", 42);
		
		m.close();
	}
	
	@After
	public void tearDown() {
		File f = new File(Metrics.getInstance().getFileName());

		assertTrue("File was not created.", f.exists());
		f.delete();
		assertFalse("File could not be deleted!", f.exists());
		
		Metrics.getInstance().close();
	}
}