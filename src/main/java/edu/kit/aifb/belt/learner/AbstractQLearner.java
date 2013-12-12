package edu.kit.aifb.belt.learner;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.kit.aifb.belt.db.Action;
import edu.kit.aifb.belt.db.QValue;
import edu.kit.aifb.belt.db.StateChain;

/**
 * Multithreading capable QLearner. Any number of threads can add jobs
 * simultaneously. Jobs are executed by a single thread.
 * 
 * @author sibbo
 */
public abstract class AbstractQLearner implements Runnable {
	private BlockingQueue<Job> queue = new ArrayBlockingQueue<Job>(100);

	private volatile boolean stop;
	private volatile Thread self;
	private Object stopLock = new Object();

	public void run() {
		while (!stop) {
			try {
				Job job = queue.poll(200, TimeUnit.MILLISECONDS);

				if (job != null) {
					if (job instanceof QLearnJob) {
						QLearnJob qJob = (QLearnJob) job;

						updateQInternal(qJob.getHistory(), qJob.getAction(), qJob.getFuture(), qJob.getReward(),
								qJob.getLearningRate(), qJob.getDiscountFactor());
					} else if (job instanceof QualityUpdateJob) {
						QualityUpdateJob quJob = (QualityUpdateJob) job;
						updateQualityInternal(quJob.getURL());
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
	 * Schedules an update q operation. Blocks until free space in the queue is
	 * available.
	 * 
	 * @param history
	 *            The state history. Entries should have a domain and a type.
	 * @param action
	 *            The action.
	 * @param future
	 *            The state future. Entries shouldn't have a domain, but a type.
	 */
	public void updateQ(StateChain history, Action action, StateChain future, double reward, double learningRate,
			double discountFactor) {
		if (!stop) {
			try {
				queue.put(new QLearnJob(history, action, future, reward, learningRate, discountFactor));
			} catch (InterruptedException e) {
				Logger.getLogger(getClass()).log(Level.FATAL, "Putting interrupted.", e);
			}
		}
	}

	public void updateQ(QValue q, double reward, double learningRate, double discountFactor) {
		updateQ(q.getHistory(), q.getAction(), q.getFuture(), reward, learningRate, discountFactor);
	}

	public void updateQuality(String url) {
		if (!stop) {
			try {
				queue.put(new QualityUpdateJob(url));
			} catch (InterruptedException e) {
				Logger.getLogger(getClass()).log(Level.FATAL, "Putting interrupted.", e);
			}
		}
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
	protected abstract void updateQInternal(StateChain history, Action action, StateChain future, double reward,
			double learningRate, double discountFactor);

	protected abstract void updateQualityInternal(String url);
}