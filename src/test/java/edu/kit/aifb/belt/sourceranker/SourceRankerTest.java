package edu.kit.aifb.belt.sourceranker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import edu.kit.aifb.belt.db.AbstractStateFactory;
import edu.kit.aifb.belt.db.Action;
import edu.kit.aifb.belt.db.Database;
import edu.kit.aifb.belt.db.QValue;
import edu.kit.aifb.belt.db.State;
import edu.kit.aifb.belt.db.StateChain;
import edu.kit.aifb.belt.db.StateFactory;
import edu.kit.aifb.belt.learner.AbstractQLearner;
import edu.kit.aifb.belt.learner.QualityMeasurementType;
import edu.kit.aifb.belt.learner.SimpleQLearner;

public class SourceRankerTest {
	private static final DecimalFormat rankFormat = new DecimalFormat("0.000");

	@Test
	public void testBuildIndex() {
		Database db = new Database();
		db.connect();

		Set<String> aSet = new HashSet<String>();
		aSet.add("a");
		Set<String> pasfsdSet = new HashSet<String>();
		pasfsdSet.add("pasfsd");

		QValue x = new QValue(new StateChain(new State("fda", aSet, db.getDictionary(), "patsft1", "paszt2"),
				new State("ax", aSet, db.getDictionary(), "pastdfx"),
				new State("a", aSet, db.getDictionary(), "pazsty")), new Action("abchg.de", "kndows",
				db.getDictionary()), new StateChain(
				new State("am", aSet, db.getDictionary(), "futusrke1", "futuhfre2"), new State("a", aSet,
						db.getDictionary(), "futurlex"), new State("ag", aSet, db.getDictionary(), "futhfurey")), 3);

		QValue y = new QValue(new StateChain(new State("pafsd", pasfsdSet, db.getDictionary()), new State("a", aSet,
				db.getDictionary(), "pastx"), new State("a", aSet, db.getDictionary(), "pasty")), new Action("abc.de",
				"knows", db.getDictionary()), new StateChain(new State("a", aSet, db.getDictionary(), "future1",
				"future2"), new State("a", aSet, db.getDictionary(), "futurex"), new State("a", aSet,
				db.getDictionary(), "futurey")), 2);

		db.updateQ(x, y);

		SourceRanker ranker = new SourceRanker(db);
	}

	@Test
	public void testRanking() {
		Database db = new Database();
		db.connect();
		AbstractQLearner learner = new SimpleQLearner(Long.MAX_VALUE, db, null, QualityMeasurementType.UNIFORM);
		learner.start();

		AbstractStateFactory sf = new StateFactory(2);
		QValue[] good = new QValue[100];
		QValue[] bad = new QValue[good.length];
		
		List<String> properties = Arrays.asList("a", "b", "c", "d");
		List<String> goodDomains = Arrays.asList("good.de", "good.com", "good.org");
		List<String> badDomains = Arrays.asList("bad.info", "bad.eu", "bad.gov");
		String property = "property";
		
		for (int i = 0; i < good.length; i++) {
			StateChain history = new StateChain(new State("x", getRandomStateProperties(properties, 2), getRandomStateProperties(properties, 2), db.getDictionary()));
			Action action = new Action(getRandomStateProperties(goodDomains, 1).iterator().next(), property, db.getDictionary());
			StateChain future = new StateChain(new State("x", getRandomStateProperties(properties, 2), getRandomStateProperties(properties, 2), db.getDictionary()));
			good[i] = new QValue(history, action, future);
			learner.updateQ("neutral.de/" + randomString(1), good[i], 0.5, 0.5, true);
			
			history = new StateChain(new State("x", getRandomStateProperties(properties, 2), getRandomStateProperties(properties, 2), db.getDictionary()));
			action = new Action(getRandomStateProperties(badDomains, 1).iterator().next(), property, db.getDictionary());
			future = new StateChain(new State("x", getRandomStateProperties(properties, 2), getRandomStateProperties(properties, 2), db.getDictionary()));
			bad[i] = new QValue(history, action, future);
			learner.updateQ("neutral.de/" + randomString(1), bad[i], 0.5, 0.5, false);
		}
		
		learner.stop();
		
		StateChain history = new StateChain(new State("x", getRandomStateProperties(properties, 2), getRandomStateProperties(properties, 2), db.getDictionary()));
		StateChain future = new StateChain(new State("x", getRandomStateProperties(properties, 2), getRandomStateProperties(properties, 2), db.getDictionary()));
		Collection<String> domains = new ArrayList<String>();
		domains.addAll(badDomains);
		domains.addAll(goodDomains);
		
		SourceRanker ranker = new SourceRanker(db);
		List<RankedDomain> result = ranker.rankSources(history, property, future, domains);
		
		assertEquals("Wrong number of domains returned.", domains.size(), result.size());
		assertTrue("Result is not sorted correctly.", isSortedBackwards(result));
		
		StringBuilder topList = new StringBuilder();
		topList.append("Ranking:");
		
		for (RankedDomain d:result) {
			topList.append('\n').append(rankFormat.format(d.getRank()) + ": " + d.getDomain());
		}
		
		Logger.getLogger(getClass()).log(Level.INFO, topList.substring(0, topList.length()));
		
		ranker.stopTimers();
		db.close();
	}

	private boolean isSortedBackwards(List<RankedDomain> result) {
		Iterator<RankedDomain> iter = result.iterator();
		RankedDomain last = iter.next();

		while (iter.hasNext()) {
			RankedDomain current = iter.next();

			if (current.getRank() > last.getRank()) {
				return false;
			}

			last = current;
		}

		return true;
	}

	private String randomString(int length) {
		char[] source = "abc".toCharArray();
		char[] target = new char[length];
		Random r = new Random();

		for (int i = 0; i < target.length; i++) {
			target[i] = source[r.nextInt(source.length)];
		}

		return String.valueOf(target);
	}

	private Set<String> getRandomStateProperties(Collection<String> source, int amount) {
		List<String> sourceList = new ArrayList<String>(source.size());
		sourceList.addAll(source);
		Set<String> result = new HashSet<String>(amount * 2);
		Random r = new Random();

		for (int i = 0; i < amount; i++) {
			result.add(sourceList.remove(r.nextInt(sourceList.size())));
		}

		return result;
	}
}