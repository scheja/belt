package edu.kit.aifb.belt.learner;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.ecyrd.speed4j.StopWatch;
import com.ecyrd.speed4j.StopWatchFactory;

import edu.kit.aifb.belt.db.Action;
import edu.kit.aifb.belt.db.Database;
import edu.kit.aifb.belt.db.QValue;
import edu.kit.aifb.belt.db.StateChain;

/**
 * Multithreading capable QLearner. Any number of threads can add jobs simultaneously. Jobs are executed by a single
 * thread.
 * 
 * @author sibbo
 */
public abstract class AbstractQLearner implements Runnable {
	private BlockingQueue<Job> queue = new ArrayBlockingQueue<Job>(100);

	private volatile boolean stop;
	private volatile boolean abort;
	private volatile Thread self;
	private Object stopLock = new Object();

	public void run() {
		StopWatch sw = StopWatchFactory.getInstance("learningTime").getStopWatch();

		while (!abort && !(stop && queue.isEmpty())) {
			try {
				Job job = queue.poll(200, TimeUnit.MILLISECONDS);

				if (job != null) {

					if (job instanceof QLearnJob) {
						sw.start();

						QLearnJob qJob = (QLearnJob) job;

						updateQInternal(qJob.getSourceURI(), qJob.getHistory(), qJob.getAction(), qJob.getFuture(),
								qJob.getLearningRate(), qJob.getDiscountFactor(), qJob.isReward());

						sw.stop();
					} else {
						Logger.getLogger(getClass()).log(Level.WARN, "Unknown Job type: " + job.getClass().getName());
					}
				}
			} catch (InterruptedException e) {
				Logger.getLogger(getClass()).log(Level.FATAL, "Polling interrupted.", e);
			}
		}

		synchronized (stopLock) {
			stopLock.notifyAll();
		}

		self = null;
	}

	public void start() {
		if (self != null) {
			return;
		}

		stop = false;

		self = new Thread(this);
		self.start();
	}

	/**
	 * Stops the Q-learning thread. Blocks until the thread terminates.
	 */
	public void stop() {
		stop = true;

		synchronized (stopLock) {
			try {
				stopLock.wait();
			} catch (InterruptedException e) {
				Logger.getLogger(getClass()).error("Waiting interrupted!", e);
			}
		}
	}

	/**
	 * Aborts the Q-learning thread. Blocks until the thread terminates.
	 */
	public void abort() {
		abort = true;
		stop = true;

		synchronized (stopLock) {
			try {
				stopLock.wait();
			} catch (InterruptedException e) {
				Logger.getLogger(getClass()).error("Waiting interrupted!", e);
			}
		}
	}

	/**
	 * Schedules an update q operation. Blocks until free space in the queue is available.
	 * 
	 * @param sourceURI
	 *            The uri of the state that is updated (Last state in the history)
	 * @param history
	 *            The state history. Entries should have a domain and a type.
	 * @param action
	 *            The action.
	 * @param future
	 *            The state future. Entries shouldn't have a domain, but a type.
	 */
	public void updateQ(String sourceURI, StateChain history, Action action, StateChain future, double learningRate,
			double discountFactor, boolean reward) {
		if (!stop) {
			try {
				queue.put(new QLearnJob(sourceURI, history, action, future, learningRate, discountFactor, reward));
			} catch (InterruptedException e) {
				Logger.getLogger(getClass()).log(Level.FATAL, "Putting interrupted.", e);
			}
		}
	}

	public void updateQ(String sourceURI, QValue q, double learningRate, double discountFactor, boolean reward) {
		updateQ(sourceURI, q.getHistory(), q.getAction(), q.getFuture(), learningRate, discountFactor, reward);
	}

	public double getRewardFromSourceURI(String sourceURI, Database db) {
		return db.getQuality(sourceURI);
	}

	public double getNegativeAverageReward(Database db) {
		return -db.getQualityMeasurement().getMean();
	}

	/**
	 * Performs an update q operation.
	 * 
	 * @param history
	 *            The state history. Entries should have a domain and a type.
	 * @param action
	 *            The action.
	 * @param future
	 *            The state future. Entries shouldn't have a domain, but a type.
	 */
	protected abstract void updateQInternal(String sourceURI, StateChain history, Action action, StateChain future,
			double learningRate, double discountFactor, boolean reward);
}