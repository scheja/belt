package edu.kit.aifb.belt.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import com.google.common.collect.AbstractIterator;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Quad;

import edu.kit.aifb.belt.db.dict.DictionaryListener;
import edu.kit.aifb.belt.db.dict.StringDictionary;
import edu.kit.aifb.belt.db.dict.StringDictionary.Entry;
import edu.kit.aifb.belt.sourceindex.SourceIndex;

/**
 * Connects to the database, provides standard functionality.
 * Not thread-safe.
 * 
 * @author sibbo
 */
public class Database implements SourceIndex, DictionaryListener {
	private static final String DRIVER = "com.mysql.jdbc.Driver";

	private static final int BATCH_SIZE = 100;

	private String host;
	private String user;
	private String password;

	private Connection connection;
	private PreparedStatement insertQStatement;
	private PreparedStatement updateQStatement;
	private PreparedStatement clearQStatement;
	private PreparedStatement getQStatement;
	private PreparedStatement getBestActionQStatement;

	private PreparedStatement insertDictStatement;

	private PreparedStatement insertQuadStatement;
	private PreparedStatement getQuadStatement;
	private PreparedStatement deleteQuadStatement;
	private PreparedStatement getQuadByContextStatement;
	private PreparedStatement replaceQuadContextStatement;

	private StringDictionary dict;
	private int dictionaryFlushThreshold = Integer.MAX_VALUE;
	
	private long size;

	/**
	 * @param host
	 *            Format: domain/dbname.
	 */
	public Database(String host) {
		this.host = host;

		try {
			Properties p = new Properties();
			InputStream in = getClass().getResourceAsStream(".password");
			p.load(in);
			in.close();

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
			stmt.execute("CREATE TABLE IF NOT EXISTS DictionaryTable (id INT PRIMARY KEY, value TEXT)");
			stmt.execute("CREATE TABLE IF NOT EXISTS SourceIndexTable (id INT PRIMARY KEY AUTO_INCREMENT, subject INT, predicate INT, object INT, context INT)");

			insertQStatement = connection
					.prepareStatement("INSERT INTO QTable (history, action, future, q, updateCount) VALUES (?, ?, ?, ?, ?)");
			updateQStatement = connection
					.prepareStatement("UPDATE QTable SET q = ?, updateCount = ? WHERE history = ? AND action = ? AND future = ?");
			clearQStatement = connection.prepareStatement("DELETE FROM QTable");
			getQStatement = connection
					.prepareStatement("SELECT q, updateCount FROM QTable WHERE history = ? AND action = ? AND future = ?");
			getBestActionQStatement = connection
					.prepareStatement("SELECT q FROM QTable WHERE history = ? ORDER BY q DESC LIMIT 1");

			insertDictStatement = connection.prepareStatement("INSERT IGNORE INTO DictionaryTable VALUES (?, ?)");

			insertQuadStatement = connection
					.prepareStatement("INSERT INTO SourceIndexTable (subject, predicate, object, context) VALUES (?, ?, ?, ?)");
			getQuadStatement = connection
					.prepareStatement("SELECT * FROM SourceIndexTable WHERE subject = ? AND predicate = ? AND object = ? and context = ?");
			deleteQuadStatement = connection
					.prepareStatement("DELETE FROM SourceIndexTable WHERE subject = ? AND predicate = ? AND object = ? AND context = ?");
			getQuadByContextStatement = connection
					.prepareStatement("SELECT subject, predicate, object, context FROM SourceIndexTable WHERE context = ?");
			replaceQuadContextStatement = connection
					.prepareStatement("UPDATE SourceIndexTable SET context = ? WHERE context = ?");

			final ResultSet entries = stmt.executeQuery("SELECT id, value FROM DictionaryTable");

			dict = new StringDictionary(this);
			
			dict.load(new AbstractIterator<Entry>() {
				@Override
				protected Entry computeNext() {
					try {
						if (entries.next()) {
							return dict.new Entry(entries.getInt(1), entries.getString(2));
						} else {
							return endOfData();
						}
					} catch (SQLException e) {
						Logger.getLogger(getClass().getName()).log(Level.WARN, "Couldn't load dictionary", e);

						return endOfData();
					}
				}
			});

			entries.close();
			stmt.close();
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
		dict = null;

		try {
			connection.close();
			connection = null;
		} catch (SQLException e) {
			throw new DatabaseException("Could not close database connection", e);
		}
	}
	
	public boolean isConnected(int timeout) {
		if (connection == null) {
			return false;
		} else {
			try {
				return connection.isValid(timeout);
			} catch (SQLException e) {
				throw new DatabaseException("Wrong value for timeout", e);
			}
		}
	}

	public void flushDictionary() {
		Iterator<Integer> entries = dict.getNewIds().iterator();

		try {
			while (entries.hasNext()) {
				for (int i = 0; i < BATCH_SIZE && entries.hasNext(); i++) {
					int entry = entries.next();
					insertDictStatement.setInt(1, entry);
					insertDictStatement.setString(2, dict.getString(entry));
					insertDictStatement.addBatch();
				}

				insertDictStatement.executeBatch();
			}
		} catch (SQLException e) {
			throw new DatabaseException("Could not flush dictionary.", e);
		}

		dict.clearNewIds();
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
		updateQ(q.getHistory().getBytes(dict), q.getAction().getBytes(dict), q.getFuture().getBytes(dict), q.getQ());
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
		double newQ = getQ(q.getHistory().getBytes(dict), q.getAction().getBytes(dict), q.getFuture().getBytes(dict));

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
		return getBestQ(history.getBytes(dict));
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
	
	public void clearQTable() {
		try {
			clearQStatement.execute();
		} catch (SQLException e) {
			throw new DatabaseException("Could not delete qtable", e);
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

	public StringDictionary getDictionary() {
		return dict;
	}

	public void addQuad(Node g, Node s, Node p, Node o) {
		try {
			getQuadStatement.setInt(1, dict.getId(s.toString()));
			getQuadStatement.setInt(2, dict.getId(p.toString()));
			getQuadStatement.setInt(3, dict.getId(o.toString()));
			getQuadStatement.setInt(4, dict.getId(g.toString()));

			ResultSet result = getQuadStatement.executeQuery();

			if (!result.next()) {
				insertQuadStatement.setInt(1, dict.getId(s.toString()));
				insertQuadStatement.setInt(2, dict.getId(p.toString()));
				insertQuadStatement.setInt(3, dict.getId(o.toString()));
				insertQuadStatement.setInt(4, dict.getId(g.toString()));

				insertQuadStatement.execute();
			}

			result.close();
		} catch (SQLException e) {
			throw new DatabaseException("Could not insert quad.", e);
		}
	}

	public void deleteQuad(Quad q) {
		try {
			deleteQuadStatement.setInt(1, dict.getId(q.getSubject().toString()));
			deleteQuadStatement.setInt(2, dict.getId(q.getPredicate().toString()));
			deleteQuadStatement.setInt(3, dict.getId(q.getObject().toString()));
			deleteQuadStatement.setInt(4, dict.getId(q.getGraph().toString()));

			deleteQuadStatement.execute();
		} catch (SQLException e) {
			throw new DatabaseException("Could not insert quad.", e);
		}
	}

	public Iterator<Quad> findAllByURI(String uri) {
		Collection<Quad> result = new ArrayList<Quad>();

		try {
			getQuadByContextStatement.setInt(1, dict.getId(uri));
			ResultSet quads = getQuadByContextStatement.executeQuery();

			while (quads.next()) {
				result.add(new Quad(Node.createURI(dict.getString(quads.getInt(4))), Node.createURI(dict
						.getString(quads.getInt(1))), Node.createURI(dict.getString(quads.getInt(2))), Node
						.createURI(dict.getString(quads.getInt(3)))));
			}

			quads.close();
		} catch (SQLException e) {
			throw new DatabaseException("Could not receive quads.", e);
		}

		return result.iterator();
	}

	public void updateURIs(String from, String to) {
		try {
			replaceQuadContextStatement.setInt(1, dict.getId(to));
			replaceQuadContextStatement.setInt(2, dict.getId(from));

			replaceQuadContextStatement.execute();
		} catch (SQLException e) {
			throw new DatabaseException("Could not update URIs.", e);
		}

	}
	
	public void setDictionaryFlushThreshold(int dictionaryFlushThreshold) {
		this.dictionaryFlushThreshold = dictionaryFlushThreshold;
	}

	public void dictionaryIdAdded() {
		if (dict.getNewIdAmount() >= dictionaryFlushThreshold ) {
			flushDictionary();
			Logger.getLogger(getClass()).log(Level.DEBUG, "Flushing dictionary. Size: " + dict.size());
		}
	}
}