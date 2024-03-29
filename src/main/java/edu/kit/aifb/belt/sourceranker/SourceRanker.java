package edu.kit.aifb.belt.sourceranker;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.googlecode.javaewah.EWAHCompressedBitmap;

import edu.kit.aifb.belt.db.Action;
import edu.kit.aifb.belt.db.Database;
import edu.kit.aifb.belt.db.QValue;
import edu.kit.aifb.belt.db.State;
import edu.kit.aifb.belt.db.StateChain;
import edu.kit.aifb.belt.metrics.Metrics;
import edu.kit.aifb.belt.metrics.Timer;

public class SourceRanker {
	/**
	 * Convention: states inside the history have negative positions (The last history state has position 0), states
	 * inside the future have positive positions.
	 */
	private final Object2ObjectMap<SRKey, SRQValue[]> qValueMap = new Object2ObjectRBTreeMap<SRKey, SRQValue[]>(
			new SRKeyComparator());
	private final BitmapTranslator propTranslator = new BitmapTranslator();
	private final BitmapTranslator typeTranslator = new BitmapTranslator();
	private final BitmapFactory bitmapFactory = new BitmapFactory(propTranslator, typeTranslator);

	private final Database db;

	private int resultsForAveraging = 5;
	private SimilarityCalculator similarityCalculator = new SimpleSimilarityCalculator();
	private QCalculator qCalculator = new AverageQCalculator();

	private Timer similarityTimer = new Timer("Similarity time");
	private Timer rankingTimer = new Timer("Ranking time");

	public SourceRanker(Database db) {
		rankingTimer.startPaused();

		this.db = db;
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
			
			int[] domainsHistory = createDomainsFromStateChain(q.getHistory());
			int[] domainsFuture = createDomainsFromStateChain(q.getFuture());
			int[] domainChain = Arrays.copyOf(domainsHistory, domainsHistory.length + domainsFuture.length);
			System.arraycopy(domainsFuture, 0, domainChain, domainsHistory.length, domainsFuture.length);

			SRQValue value = new SRQValue(propChain, typeChain, domainChain, futureOffset, q.getQ());
			Action action = q.getAction();

			// Insert states
			for (int i = 0; i < propChain.length; i++) {
				// Last history state will have number 0.
				int number = i - futureOffset + 1;

				for (int prop : propChain[i]) {
					SRKey key = new SRKey(action, prop, number);
					put(key, value);
				}

				for (int type : typeChain[i]) {
					SRKey key = new SRKey(action, type, number);
					put(key, value);
				}
			}
		}
	}

	private int[] createDomainsFromStateChain(StateChain chain) {
		int[] result = new int[chain.size()];

		for (int i = 0; i < chain.size(); i++) {
			result[i] = chain.getStateList().get(i).getDomain();
		}

		return result;
	}

	public void setResultsForAveraging(int resultsForAveraging) {
		this.resultsForAveraging = resultsForAveraging;
	}

	private void put(SRKey key, SRQValue value) {
		SRQValue[] current = qValueMap.get(key);

		if (current == null) {
			qValueMap.put(key, new SRQValue[] { value });
		} else {
			// Search for free place.
			int free = 0;
			for (; free < current.length; free++) {
				if (current[free] == null) {
					break;
				}
			}

			// If no room left, create new array.
			if (current.length == free) {
				SRQValue[] replacement = Arrays.copyOf(current, current.length << 1);
				replacement[free] = value;
				qValueMap.put(key, replacement);
			} else {
				current[free] = value;
			}
		}
	}

	private EWAHCompressedBitmap[][] createBitmapsFromStateChain(StateChain chain) {
		EWAHCompressedBitmap[][] result = new EWAHCompressedBitmap[2][chain.size()];

		for (int i = 0; i < result[0].length; i++) {
			EWAHCompressedBitmap[] tmp = bitmapFactory.getBitmaps(chain.getStateList().get(i));

			result[0][i] = tmp[0];
			result[1][i] = tmp[1];
		}

		return result;
	}

	@SuppressWarnings("unused")
	private StateChain[] createStateChainFromBitmaps(EWAHCompressedBitmap[] props, EWAHCompressedBitmap[] types,
			int futureOffset, int domain) {
		List<State> history = new ArrayList<State>(futureOffset);
		List<State> future = new ArrayList<State>(props.length - futureOffset);

		// Create state chains.
		for (int i = 0; i < props.length; i++) {
			int[] stateProps = new int[props[i].cardinality()];
			int index = 0;

			// Get all property ids.
			for (int p : props[i]) {
				stateProps[index++] = propTranslator.getInt(p);
			}

			int[] stateTypes = new int[types[i].cardinality()];
			index = 0;

			// Get all type ids.
			for (int t : types[i]) {
				stateTypes[index++] = typeTranslator.getInt(t);
			}

			State state = new State(domain, stateTypes, stateProps);

			if (i < futureOffset) {
				history.add(state);
			} else {
				future.add(state);
			}
		}

		return new StateChain[] { new StateChain(history), new StateChain(future) };
	}

	public List<RankedDomain> rankSources(StateChain history, String actionProperty, StateChain future,
			Collection<String> domains) {
		rankingTimer.unpause();

		List<RankedDomain> result = new ArrayList<RankedDomain>();

		// Combine state chains.
		List<State> combined = new ArrayList<State>();
		combined.addAll(history.getStateList());
		combined.addAll(future.getStateList());
		final int futureOffset = history.size();

		EWAHCompressedBitmap[][] bitmaps = createBitmapsFromStateChain(new StateChain(combined));
		int[] domainChain = createDomainsFromStateChain(new StateChain(combined));

		for (String domain : domains) {
			double q = getQValue(bitmaps[0], bitmaps[1], domainChain, futureOffset, domain, actionProperty);
			result.add(new RankedDomain(domain, q));
		}

		Collections.sort(result);
		rankingTimer.pause();

		return result;
	}

	private double getQValue(EWAHCompressedBitmap[] props, EWAHCompressedBitmap[] types, int[] domains, final int futureOffset,
			String domain, String actionProperty) {
		ObjectSet<SRResultValue> candidates = new ObjectRBTreeSet<SRResultValue>();
		final Action action = new Action(domain, actionProperty, db.getDictionary());

		// For every state in the chain ...
		for (int i = 0; i < props.length; i++) {
			final int number = i - futureOffset + 1;

			// ... and for every property and type retrieve the mapping, if
			// it exists.
			for (int prop : props[i]) {
				SRKey key = new SRKey(action, prop, number);
				SRQValue[] values = qValueMap.get(key);

				if (values != null) {
					// Add all the results to the result set.
					for (SRQValue value : values) {
						if (value == null) {
							break;
						}
						
						candidates.add(new SRResultValue(key, value));
					}
				}
			}

			// The mapping for the types.
			for (int type : types[i]) {
				SRKey key = new SRKey(action, type, number);
				SRQValue[] values = qValueMap.get(key);

				if (values != null) {
					// Add all results to the result set.
					for (SRQValue value : values) {
						if (value == null) {
							break;
						}
						
						candidates.add(new SRResultValue(key, value));
					}
				}
			}
		}

		similarityTimer.startPaused();
		List<SRResultValue> results = new ArrayList<SRResultValue>(candidates.size());

		for (SRResultValue value : candidates) {
			similarityTimer.unpause();
			value.setSimilarity(similarityCalculator.calculateSimilarity(props, types, futureOffset, domains, value.getProps(),
					value.getTypes(), value.getFutureOffset(), value.getDomains()));
			similarityTimer.pause();

			results.add(value);
		}

		similarityTimer.stop();
		
		Metrics.getInstance().saveTimer("Similarity calculation time (One lap is one state, one line is one ranking process)", similarityTimer);

		Collections.sort(results, new SRResultValueSimilarityComparator());
		List<SRResultValue> calculationMembers = results.subList(0, Math.min(resultsForAveraging, results.size()));

		return qCalculator.calculateQ(calculationMembers);
	}
	
	public void stopTimers() {
		rankingTimer.stop();
		
		Metrics.getInstance().saveTimer("Ranking Timer (One lap is one ranking process)", rankingTimer);
	}
}
