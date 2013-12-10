package edu.kit.aifb.belt.sourceranker;

import java.util.Iterator;

import com.googlecode.javaewah.EWAHCompressedBitmap;

import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import edu.kit.aifb.belt.db.Action;
import edu.kit.aifb.belt.db.Database;
import edu.kit.aifb.belt.db.QValue;
import edu.kit.aifb.belt.db.State;
import edu.kit.aifb.belt.db.StateChain;

public class StateStore {
	/**
	 * Convention: states inside the history have negative positions, states
	 * inside the future have positive positions.
	 */
	private Object2ObjectMap<SRKey, SRQValue> stateChainMap = new Object2ObjectRBTreeMap<SRKey, SRQValue>(
			new SRKeyComparator());
	private BitmapTranslator propTranslator = new BitmapTranslator();
	private BitmapTranslator typeTranslator = new BitmapTranslator();
	private BitmapFactory bitmapFactory = new BitmapFactory(propTranslator, typeTranslator);

	public StateStore(Database db) {
		Iterator<QValue> iter = db.listAllQs();
		
		while (iter.hasNext()) {
			QValue q = iter.next();
			
			EWAHCompressedBitmap[] history = createBitmapsFromStateChain(q.getHistory());
			EWAHCompressedBitmap[] future = createBitmapsFromStateChain(q.getFuture());
			
			//SRQValue value = new SRQValue(history, future, q.getQ());
			
			// Insert history states
			for (int i = 0; i < history.length; i++) {
				for (int propOrType : history[i]) {
					
				}
			}
		}
	}

	/**
	 * Traverses db to get all props and types and set up the maps for the bitset.
	 * @param db
	 */
	private void loadPropsAndTypes(Database db) {
		Iterator<QValue> iter = db.listAllQs();
		IntSet props = new IntRBTreeSet();
		IntSet types = new IntRBTreeSet();

		// Get all props and types
		while (iter.hasNext()) {
			QValue q = iter.next();

			addPropsAndTypes(q.getHistory(), props, types);
			addPropsAndTypes(q.getFuture(), props, types);
		}

		// Set up bitmap index with props and types
		translator = new BitmapTranslator();

		for (int prop : props) {
			translator.addInt(prop);
		}

		for (int type : types) {
			translator.addInt(type);
		}

		propCount = props.size();
		typeCount = types.size();
		bitmapFactory = new BitmapFactory(translator);
	}

	private void addPropsAndTypes(StateChain chain, IntSet props, IntSet types) {
		for (State s : chain.getStateList()) {
			for (int prop : s.getProperties()) {
				props.add(prop);
			}

			for (int type : s.getTypes()) {
				types.add(type);
			}
		}
	}

	private EWAHCompressedBitmap[] createBitmapsFromStateChain(StateChain chain) {
		EWAHCompressedBitmap[] result = new EWAHCompressedBitmap[chain.size()];

		for (int i = 0; i < result.length; i++) {
			result[i] = bitmapFactory.getBitmap(chain.getStateList().get(i));
		}

		return result;
	}
}