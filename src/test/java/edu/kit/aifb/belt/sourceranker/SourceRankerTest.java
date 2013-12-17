package edu.kit.aifb.belt.sourceranker;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import edu.kit.aifb.belt.db.Action;
import edu.kit.aifb.belt.db.Database;
import edu.kit.aifb.belt.db.QValue;
import edu.kit.aifb.belt.db.State;
import edu.kit.aifb.belt.db.StateChain;

public class SourceRankerTest {
	@Test
	public void testBuildIndex() {
		Database db = new Database();
		db.connect();
		
		Set<String> aSet = new HashSet<String>();
		aSet.add("a");
		Set<String> pasfsdSet = new HashSet<String>();
		pasfsdSet.add("pasfsd");
		
		QValue x = new QValue(new StateChain(new State("fda", aSet, db.getDictionary(), "patsft1", "paszt2"), new State("ax",
				aSet, db.getDictionary(), "pastdfx"), new State("a", aSet, db.getDictionary(), "pazsty")), new Action(
				"abchg.de", "kndows", db.getDictionary()), new StateChain(new State("am", aSet, db.getDictionary(),
				"futusrke1", "futuhfre2"), new State("a", aSet, db.getDictionary(), "futurlex"), new State("ag", aSet,
				db.getDictionary(), "futhfurey")), 3);

		QValue y = new QValue(new StateChain(new State("pafsd", pasfsdSet, db.getDictionary()), new State("a", aSet,
				db.getDictionary(), "pastx"), new State("a", aSet, db.getDictionary(), "pasty")), new Action("abc.de",
				"knows", db.getDictionary()), new StateChain(new State("a", aSet, db.getDictionary(), "future1",
				"future2"), new State("a", aSet, db.getDictionary(), "futurex"), new State("a", aSet,
				db.getDictionary(), "futurey")), 2);
		
		db.updateQ(x, y);
		
		SourceRanker ranker = new SourceRanker(db);
	}
}