package edu.kit.aifb.belt.metrics;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Metrics {
	private static final Metrics INSTANCE = new Metrics();

	public static Metrics getInstance() {
		return INSTANCE;
	}

	private static final String DRIVER = "org.sqlite.JDBC";
	private final Logger log = LogManager.getLogger(getClass());

	private String file = "metrics - "
			+ new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date(
					System.currentTimeMillis())) + ".db";
	private Connection connection;
	private Statement stmt;

	private PreparedStatement updateParameterStatement;
	private PreparedStatement insertParameterStatement;

	private Set<String> metrics = new HashSet<String>();

	/** Use getInstance()! */
	private Metrics() {

	}

	private void checkConnection() {
		if (connection == null) {
			try {
				Class.forName(DRIVER);
				connection = DriverManager.getConnection("jdbc:sqlite:" + file);
				stmt = connection.createStatement();

				stmt.execute("CREATE TABLE IF NOT EXISTS Parameters (name CHAR(256) PRIMARY KEY, value INT)");

				updateParameterStatement = connection
						.prepareStatement("UPDATE Parameters SET value = ? WHERE name = ?");
				insertParameterStatement = connection
						.prepareStatement("INSERT INTO Parameters (name, value) VALUES (?, ?)");
			} catch (ClassNotFoundException e) {
				log.log(Level.FATAL, "No sqlite driver found!", e);
			} catch (SQLException e) {
				log.log(Level.ERROR, "Could not connect to sqlite database.", e);
			}
		}
	}

	/**
	 * Save metric types that only have one value.
	 * 
	 * @param name
	 * @param value
	 */
	public void setParameter(String name, int value) {
		checkConnection();

		try {
			updateParameterStatement.setInt(1, value);
			updateParameterStatement.setString(2, name);

			if (updateParameterStatement.executeUpdate() == 0) {
				insertParameterStatement.setString(1, name);
				insertParameterStatement.setInt(2, value);
				insertParameterStatement.execute();
			}
		} catch (SQLException e) {
			log.log(Level.FATAL, "Could not execute query!", e);
		}
	}

	public String getFileName() {
		return file;
	}

	public void close() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				log.log(Level.FATAL, "Could not close database!", e);
			}

			connection = null;
		}
	}

	public void saveTimer(String name, Timer timer) {
		checkConnection();

		try {
			stmt.execute("CREATE TABLE IF NOT EXISTS '"
					+ name
					+ "' (id INTEGER PRIMARY KEY AUTOINCREMENT, laps INT, nanoTime INT)");

			stmt.execute("INSERT INTO '" + name + "' (laps, nanoTime) VALUES ("
					+ timer.getLaps() + ", " + timer.getElapsedNanos() + ")");
		} catch (SQLException e) {
			log.log(Level.FATAL, "Could not execute query!", e);
		}
	}

	public void createMetric(String name) {
		if (metrics.contains(name)) {
			throw new IllegalArgumentException("Metric " + name
					+ " was already created");
		}

		checkConnection();

		try {
			stmt.execute("CREATE TABLE '" + name
					+ "' (id INTEGER PRIMARY KEY AUTOINCREMENT, value INT)");

			metrics.add(name);
		} catch (SQLException e) {
			log.log(Level.FATAL, "Could not execute query!", e);
		}
	}

	public void insertValue(String metric, int value) {
		if (!metrics.contains(metric)) {
			throw new IllegalArgumentException("Metric " + metric
					+ " does not exist.");
		}

		try {
			stmt.execute("INSERT INTO '" + metric + "' (value) VALUES ("
					+ value + ")");
		} catch (SQLException e) {
			log.log(Level.FATAL, "Could not execute query!", e);
		}
	}
}