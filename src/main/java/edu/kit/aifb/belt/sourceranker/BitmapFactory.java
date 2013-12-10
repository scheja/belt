package edu.kit.aifb.belt.sourceranker;

import java.util.HashMap;
import java.util.Map;

import com.googlecode.javaewah.EWAHCompressedBitmap;

import edu.kit.aifb.belt.db.State;

public class BitmapFactory {
	private final Map<State, EWAHCompressedBitmap[]> bitmaps = new HashMap<State, EWAHCompressedBitmap[]>();
	private final BitmapTranslator propTranslator;
	private final BitmapTranslator typeTranslator;
	
	public BitmapFactory(BitmapTranslator propTranslator, BitmapTranslator typeTranslator) {
		this.propTranslator = propTranslator;
		this.typeTranslator = typeTranslator;
	}
	
	public EWAHCompressedBitmap[] getBitmaps(State s) {
		EWAHCompressedBitmap[] result = bitmaps.get(s);
		
		if (result != null) {
			return result;
		}
		
		result = new EWAHCompressedBitmap[2];
		
		for (int prop : s.getProperties()) {
			result[0].set(propTranslator.getBitmap(prop));
		}
		
		for (int type : s.getTypes()) {
			result[1].set(typeTranslator.getBitmap(type));
		}
		
		bitmaps.put(s, result);
		
		return result;
	}
}