package edu.kit.aifb.belt.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.PreparedStatement;

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

	private StringDictionary stateDict = new StringDictionary();

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
			stmt.execute("CREATE TABLE IF NOT EXISTS QTable (id INT PRIMARY KEY AUTO_INCREMENT, history BIGINT, action BIGINT, future BIGINT, q DOUBLE, updateCount INT)");
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

			stateDict.load(new AbstractIterator<Entry>() {
				@Override
				protected Entry computeNext() {
					try {
						if (dict.next()) {
							return stateDict.new Entry(dict.getLong(1), dict.getString(2));
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
		Iterator<Long> entries = stateDict.getNewIds().iterator();

		try {
			while (entries.hasNext()) {
				for (int i = 0; i < BATCH_SIZE && entries.hasNext(); i++) {
					long entry = entries.next();
					insertDictStatement.setLong(1, entry);
					insertDictStatement.setString(2, stateDict.getString(entry));
					insertDictStatement.addBatch();
				}
				
				insertDictStatement.executeBatch();
			}
		} catch (SQLException e) {
			throw new DatabaseException("Could not flush dictionary.", e);
		}
		
		stateDict.clearNewIds();
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
		try {
			getQStatement.setString(1, q.getHistory().toString());
			getQStatement.setString(2, q.getAction().toString());
			getQStatement.setString(3, q.getFuture().toString());

			ResultSet result = getQStatement.executeQuery();

			if (result.next()) {
				updateQStatement.setDouble(1, q.getQ());
				updateQStatement.setInt(2, result.getInt(2));
				updateQStatement.setString(3, q.getHistory().toString());
				updateQStatement.setString(4, q.getAction().toString());
				updateQStatement.setString(5, q.getFuture().toString());

				int updateCount = updateQStatement.executeUpdate();

				if (updateCount != 1) {
					throw new DatabaseException("Updated a wrong number of rows: " + updateCount + " (should be: ");
				}
			} else {
				insertQStatement.setString(1, q.getHistory().toString());
				insertQStatement.setString(2, q.getAction().toString());
				insertQStatement.setString(3, q.getFuture().toString());
				insertQStatement.setDouble(4, q.getQ());
				insertQStatement.setInt(5, 0);

				insertQStatement.execute();

				// Increase size: 3 equals two ids for action and one double for
				// q.
				size += (q.getHistory().size() + 3 + q.getFuture().size()) << 3;
			}

			result.close();
		} catch (SQLException e) {
			throw new DatabaseException("Could not update Q values.", e);
		}
	}

	/**
	 * Sets the q value of the given {@link QValue} to the value stored in the
	 * database.
	 * 
	 * @return True if the q value was found in the database, false otherwise.
	 */
	public boolean getQ(QValue q) {
		try {
			getQStatement.setString(1, q.getHistory().toString());
			getQStatement.setString(2, q.getAction().toString());
			getQStatement.setString(3, q.getFuture().toString());

			ResultSet result = getQStatement.executeQuery();

			if (result.next()) {
				q.setQ(result.getDouble(1));

				result.close();
				return true;
			} else {
				result.close();
				return false;
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
		try {
			getBestActionQStatement.setString(1, history.toString());

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