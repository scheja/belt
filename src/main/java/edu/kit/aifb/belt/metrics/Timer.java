package edu.kit.aifb.belt.metrics;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Timer {
	private final String name;
	private final Logger log;

	private long startNanos;
	private long lapNanos;
	private long elapsedNanos;
	private long laps;

	private boolean started;
	private boolean paused;

	public Timer(String name) {
		Objects.requireNonNull(name, "'message' must not be null.");

		this.name = name;
		log = Logger.getLogger(name);
	}

	public Timer() {
		name = "";
		log = null;
	}

	public String getName() {
		return name;
	}

	public long getStartNanos() {
		return startNanos;
	}

	public long getLapNanos() {
		return lapNanos;
	}

	public long getElapsedNanos() {
		return elapsedNanos;
	}

	public long getLaps() {
		return laps;
	}

	public boolean isStarted() {
		return started;
	}

	public boolean isPaused() {
		return paused;
	}

	public void start() {
		if (started) {
			throw new IllegalStateException("Timer already started");
		}

		started = true;
		paused = false;
		elapsedNanos = 0;
		laps = 1;
		lapNanos = startNanos = System.nanoTime();
	}

	public void startPaused() {
		if (started) {
			throw new IllegalStateException("Timer already started");
		}

		started = true;
		paused = true;
		elapsedNanos = 0;
		laps = 0;
		startNanos = System.nanoTime();
	}

	public void pause() {
		if (!started) {
			throw new IllegalStateException("Timer not running");
		}

		if (paused) {
			throw new IllegalStateException("Timer already paused");
		}

		paused = true;
		elapsedNanos += System.nanoTime() - lapNanos;
	}

	public void unpause() {
		if (!started) {
			throw new IllegalStateException("Timer not running");
		}

		if (!paused) {
			throw new IllegalStateException("Timer not paused");
		}

		paused = false;
		laps++;
		lapNanos = System.nanoTime();
	}

	public void stop() {
		if (!started) {
			throw new IllegalStateException("Timer not running");
		}

		if (!paused) {
			pause();
		}

		if (log != null) {
			long endNanos = System.nanoTime();

			StringBuilder str = new StringBuilder();

			str.append("Timer '").append(name).append("': \n");
			str.append("Total time: ").append(format(endNanos - startNanos))
					.append('\n');
			str.append("Working time: ").append(format(elapsedNanos))
					.append('\n');

			if (laps > 0) {
				str.append("Lap count: ").append(laps).append('\n');
				str.append("Average lap time: ")
						.append(format(elapsedNanos / laps)).append('\n');
				str.append("Average laps per second: ")
						.append(laps * 1000000000 / elapsedNanos).append('\n');
			}

			log.log(Level.INFO, str);
		}
	}

	private String format(long time) {
		if (time == 0) {
			return "0 s";
		}

		String postFix;
		long absTime = Math.abs(time);
		long logTime = (long) Math.log10(absTime);
		long quotient;

		if (absTime < 10000) {
			postFix = " ns";
			quotient = 1;
		} else if (absTime < 10000000) {
			postFix = " us";
			quotient = 1000;
		} else if (logTime < 10000000000L) {
			postFix = " ms";
			quotient = 1000000;
		} else {
			Date date = new Date(time);
			return new SimpleDateFormat("HH:mm:ss").format(date) + " h";
		}

		String number = String.valueOf(time / quotient);

		if (number.length() > 3) {
			number = number.substring(0, number.length() - 3) + ","
					+ number.substring(number.length() - 3);
		}

		return number + postFix;
	}
}