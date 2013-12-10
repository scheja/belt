package edu.kit.aifb.belt.sourceranker;

import java.util.Arrays;
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
	private Object2ObjectMap<SRKey, SRQValue> qValueMap = new Object2ObjectRBTreeMap<SRKey, SRQValue>(
			new SRKeyComparator());
	private BitmapTranslator propTranslator = new BitmapTranslator();
	private BitmapTranslator typeTranslator = new BitmapTranslator();
	private BitmapFactory bitmapFactory = new BitmapFactory(propTranslator, typeTranslator);

	public StateStore(Database db) {
		Iterator<QValue> iter = db.listAllQs();
		
		while (iter.hasNext()) {
			QValue q = iter.next();
			
			EWAHCompressedBitmap[][] history = createBitmapsFromStateChain(q.getHistory());
			EWAHCompressedBitmap[][] future = createBitmapsFromStateChain(q.getFuture());
			EWAHCompressedBitmap[] propChain = Arrays.copyOf(history[0], history[0].length + future[0].length);
			EWAHCompressedBitmap[] typeChain = Arrays.copyOf(history[1], history[1].length + future[1].length);
			System.arraycopy(future[0], 0, propChain, history[0].length, future[0].length);
			System.arraycopy(future[1], 0, typeChain, history[1].length, future[1].length);
			int futureOffset = history[0].length;
			
			SRQValue value = new SRQValue(propChain, typeChain, futureOffset, q.getQ());
			Action action = q.getAction();
			
			// Insert states
			for (int i = 0; i < propChain.length; i++) {
				// Last history state will have number 0.
				int number =  i - futureOffset + 1;
				
				for (int prop : propChain[i]) {
					SRKey key = new SRKey(action, prop, number);
					qValueMap.put(key, value);
				}
				
				for (int type : typeChain[i]) {
					SRKey key = new SRKey(action, type, number);
					qValueMap.put(key, value);
				}
			}
		}
	}

	private EWAHCompressedBitmap[][] createBitmapsFromStateChain(StateChain chain) {
		EWAHCompressedBitmap[][] result = new EWAHCompressedBitmap[2][chain.size()];

		for (int i = 0; i < result.length; i++) {
			EWAHCompressedBitmap[] tmp = bitmapFactory.getBitmaps(chain.getStateList().get(i));
			result[0][i] = tmp[0];
			result[1][i] = tmp[1];
		}

		return result;
	}
}