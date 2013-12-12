package edu.kit.aifb.belt.sourceranker;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.List;

public class BitmapTranslator {
	private final IntList bitmapToInt = new IntArrayList();
	private final Int2IntMap intToBitmap = new Int2IntRBTreeMap();

	/**
	 * Adds a new id.
	 * @param i
	 * @return
	 */
	public int addInt(int i) {
		bitmapToInt.add(i);
		intToBitmap.put(i, bitmapToInt.size());
		return bitmapToInt.size();
	}

	/**
	 * Retrieves an id that is represented by the given bitmap index.
	 * @param bitmap
	 * @return
	 */
	public int getInt(int bitmap) {
		return bitmapToInt.getInt(bitmap);
	}

	/**
	 * Retrieves the bitmap index for the given id.
	 * @param i
	 * @return
	 */
	public int getBitmap(int i) {
		if (!intToBitmap.containsKey(i)) {
			return addInt(i);
		} else {
			return intToBitmap.get(i);
		}
	}

	public int size() {
		return bitmapToInt.size();
	}

	public boolean containsInt(int i) {
		return intToBitmap.containsKey(i);
	}
}