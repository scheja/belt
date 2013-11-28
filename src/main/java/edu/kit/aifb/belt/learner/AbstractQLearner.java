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
	private BlockingQueue<QLearnJob> queue = new ArrayBlockingQueue<QLearnJob>(100);

	private volatile boolean stop;
	private volatile Thread self;

	public void run() {
		while (!stop) {
			try {
				QLearnJob job = queue.poll(200, TimeUnit.MILLISECONDS);

				if (job != null) {
					updateQInternal(job.getHistory(), job.getAction(), job.getFuture(), job.getReward(),
							job.getLearningRate(), job.getDiscountFactor());
				}
			} catch (InterruptedException e) {
				Logger.getLogger(getClass()).log(Level.FATAL, "Polling interrupted.", e);
			}
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

	public void stop() {
		stop = true;
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
			queue.offer(new QLearnJob(history, action, future, reward, learningRate, discountFactor));
		}
	}

	public void updateQ(QValue q, double reward, double learningRate, double discountFactor) {
		if (!stop) {
			updateQ(q.getHistory(), q.getAction(), q.getFuture(), reward, learningRate, discountFactor);
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
}