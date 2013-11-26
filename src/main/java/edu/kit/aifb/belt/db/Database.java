package edu.kit.aifb.belt.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.AbstractIterator;

import edu.kit.aifb.belt.db.dict.StringDictionary;
import edu.kit.aifb.belt.db.dict.StringDictionary.Entry;

/**
 * Connects to the database, provides standard functionality.
 * 
 * @author sibbo
 */
public class Database {
	private static final String DRIVER = "com.mysql.jdbc.Driver";

	private static final int BATCH_SIZE = 100;

	private String host;
	private String user;
	private String password;

	private Connection connection;
	private PreparedStatement insertQStatement;
	private PreparedStatement updateQStatement;
	private PreparedStatement getQStatement;
	private PreparedStatement getBestActionQStatement;
	private PreparedStatement insertDictStatement;

	private StringDictionary stringDict = new StringDictionary();

	private long size;

	/**
	 * @param host
	 *            Format: domain/dbname.
	 */
	public Database(String host) {
		this.host = host;

		try {
			Properties p = new Properties();
			p.load(getClass().getResourceAsStream(".password"));

			user = p.getProperty("user");
			password = p.getProperty("password");

			if (user == null || password == null) {
				throw new DatabaseException("Password file needs a user and a password entry.");
			}
		} catch (IOException e) {
			throw new DatabaseException("Missing password file.", e);
		}
	}

	public Database(String host, String user, String password) {
		this.host = host;
		this.user = user;
		this.password = password;
	}

	/**
	 * Returns a new {@code Database} object with the same host, user and
	 * password. The new object is not connected.
	 */
	public Database clone() {
		return new Database(host, user, password);
	}

	public void connect() {
		if (connection != null) {
			return;
		}

		try {
			Class.forName(DRIVER);

			connection = DriverManager.getConnection("jdbc:mysql://" + host, user, password);

			// Create tables
			Statement stmt = connection.createStatement();
			stmt.execute("CREATE TABLE IF NOT EXISTS QTable (id INT PRIMARY KEY AUTO_INCREMENT, history BLOB, action BLOB, future BLOB, q DOUBLE, updateCount INT)");
			stmt.execute("CREATE TABLE IF NOT EXISTS DictionaryTable (id BIGINT PRIMARY KEY, value TEXT)");

			insertQStatement = connection
					.prepareStatement("INSERT INTO QTable (history, action, future, q, updateCount) VALUES (?, ?, ?, ?, ?)");
			updateQStatement = connection
					.prepareStatement("UPDATE QTable SET q = ?, updateCount = ? WHERE history = ? AND action = ? AND future = ?");
			getQStatement = connection
					.prepareStatement("SELECT q, updateCount FROM QTable WHERE history = ? AND action = ? AND future = ?");
			getBestActionQStatement = connection
					.prepareStatement("SELECT q FROM QTable WHERE history = ? ORDER BY q DESC LIMIT 1");

			insertDictStatement = connection.prepareStatement("INSERT IGNORE INTO DictionaryTable (?, ?)");

			final ResultSet dict = stmt.executeQuery("SELECT id, value FROM DictionaryTable");

			stringDict.load(new AbstractIterator<Entry>() {
				@Override
				protected Entry computeNext() {
					try {
						if (dict.next()) {
							return stringDict.new Entry(dict.getLong(1), dict.getString(2));
						} else {
							return endOfData();
						}
					} catch (SQLException e) {
						Logger.getLogger(getClass().getName()).log(Level.WARNING, "Couldn't load dictionary", e);

						return endOfData();
					}
				}
			});

			dict.close();
		} catch (ClassNotFoundException e) {
			throw new DatabaseException("Could not find driver: " + DRIVER, e);
		} catch (SQLException e) {
			throw new DatabaseException("Could not connect to db: " + host, e);
		}
	}

	public void close() {
		if (connection == null) {
			return;
		}

		flushDictionary();

		try {
			connection.close();
			connection = null;
		} catch (SQLException e) {
			throw new DatabaseException("Could not close database connection", e);
		}
	}

	public void flushDictionary() {
		Iterator<Long> entries = stringDict.getNewIds().iterator();

		try {
			while (entries.hasNext()) {
				for (int i = 0; i < BATCH_SIZE && entries.hasNext(); i++) {
					long entry = entries.next();
					insertDictStatement.setLong(1, entry);
					insertDictStatement.setString(2, stringDict.getString(entry));
					insertDictStatement.addBatch();
				}

				insertDictStatement.executeBatch();
			}
		} catch (SQLException e) {
			throw new DatabaseException("Could not flush dictionary.", e);
		}

		stringDict.clearNewIds();
	}

	public void updateQ(Collection<QValue> qs) {
		for (QValue q : qs) {
			updateQ(q);
		}
	}

	public void updateQ(QValue... qs) {
		for (QValue q : qs) {
			updateQ(q);
		}
	}

	public void updateQ(QValue q) {
		updateQ(q.getHistory().getBytes(stringDict), q.getAction().getBytes(stringDict),
				q.getFuture().getBytes(stringDict), q.getQ());
	}

	public void updateQ(byte[] history, byte[] action, byte[] future, double q) {
		try {
			getQStatement.setBytes(1, history);
			getQStatement.setBytes(2, action);
			getQStatement.setBytes(3, future);

			ResultSet result = getQStatement.executeQuery();

			if (result.next()) {
				updateQStatement.setDouble(1, q);
				updateQStatement.setInt(2, result.getInt(2));
				updateQStatement.setBytes(3, history);
				updateQStatement.setBytes(4, action);
				updateQStatement.setBytes(5, future);

				int updateCount = updateQStatement.executeUpdate();

				if (updateCount != 1) {
					throw new DatabaseException("Updated a wrong number of rows: " + updateCount + " (should be: ");
				}
			} else {
				insertQStatement.setBytes(1, history);
				insertQStatement.setBytes(2, action);
				insertQStatement.setBytes(3, future);
				insertQStatement.setDouble(4, q);
				insertQStatement.setInt(5, 0);

				insertQStatement.execute();

				// Increase size: 3 equals two ids for action and one double for
				// q.
				size += history.length + history.length + 3 * 8;
			}

			result.close();
		} catch (SQLException e) {
			throw new DatabaseException("Could not update Q values.", e);
		}
	}

	public boolean getQ(QValue q) {
		double newQ = getQ(q.getHistory().getBytes(stringDict), q.getAction().getBytes(stringDict), q.getFuture()
				.getBytes(stringDict));

		if (Double.isNaN(newQ)) {
			return false;
		} else {
			q.setQ(newQ);
			return true;
		}
	}

	/**
	 * Sets the q value of the given {@link QValue} to the value stored in the
	 * database.
	 * 
	 * @return True if the q value was found in the database, false otherwise.
	 */
	public double getQ(byte[] history, byte[] action, byte[] future) {
		try {
			getQStatement.setBytes(1, history);
			getQStatement.setBytes(2, action);
			getQStatement.setBytes(3, future);

			ResultSet result = getQStatement.executeQuery();

			if (result.next()) {
				double q = result.getDouble(1);

				result.close();
				return q;
			} else {
				result.close();
				return Double.NaN;
			}
		} catch (SQLException e) {
			throw new DatabaseException("Error while fetching q value.", e);
		}
	}

	/**
	 * Returns the best available q value for the given history.
	 * 
	 * @param history
	 *            The history.
	 * @return The best available q value for the given history, or NaN, if no q
	 *         value was found.
	 */
	public double getBestQ(StateChain history) {
		return getBestQ(history.getBytes(stringDict));
	}
	
	public double getBestQ(byte[] history) {
		try {
			getBestActionQStatement.setBytes(1, history);

			ResultSet result = getBestActionQStatement.executeQuery();

			if (result.next()) {
				double q = result.getDouble(1);
				result.close();
				return q;
			} else {
				result.close();
				return Double.NaN;
			}
		} catch (SQLException e) {
			throw new DatabaseException("Error while fetching best q value.", e);
		}
	}

	/**
	 * Returns the size of the stored values in byte.
	 * 
	 * @return The size of the stored values in byte.
	 */
	public long getSize() {
		return size;
	}
}