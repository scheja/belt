package edu.kit.aifb.belt.db;

import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.ByteArrayInputStream;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.common.collect.AbstractIterator;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Quad;

import edu.kit.aifb.belt.db.dict.DictionaryListener;
import edu.kit.aifb.belt.db.dict.StringDictionary;
import edu.kit.aifb.belt.db.dict.StringDictionary.Entry;
import edu.kit.aifb.belt.db.quality.QualityMeasurement;
import edu.kit.aifb.belt.db.quality.UniformQualityMeasurement;
import edu.kit.aifb.belt.metrics.Metrics;
import edu.kit.aifb.belt.metrics.Timer;
import edu.kit.aifb.belt.sourceindex.SourceIndex;

/**
 * Connects to the database, provides standard functionality. Not thread-safe.
 * 
 * @author sibbo
 */
public class Database implements SourceIndex, DictionaryListener {
	private static final String DRIVER = "com.mysql.jdbc.Driver";

	private static final int BATCH_SIZE = 100;

	/** Size of a bitmap for a cardinality from 0 to 100. */
	private static final int[] sizeMap = new int[] { 19, 27, 43, 58, 74, 89,
			103, 118, 133, 147, 161, 175, 189, 202, 216, 229, 242, 255, 267,
			280, 293, 305, 317, 329, 341, 353, 364, 375, 387, 398, 409, 420,
			430, 441, 451, 462, 471, 482, 492, 501, 511, 520, 530, 539, 548,
			558, 566, 575, 584, 593, 601, 609, 618, 626, 634, 642, 649, 657,
			665, 673, 680, 687, 695, 702, 708, 715, 723, 730, 737, 743, 750,
			756, 763, 768, 775, 781, 788, 793, 799, 805, 811, 816, 822, 828,
			834, 838, 844, 849, 855, 860, 864, 869, 875, 880, 884, 889, 894,
			899, 902, 907, 912 };

	private String host;
	private String database;
	private String user;
	private String password;

	private Connection connection;
	private PreparedStatement insertQStatement;
	private PreparedStatement updateQStatement;
	private PreparedStatement clearQStatement;
	private PreparedStatement getQStatement;
	private PreparedStatement getBestActionQStatement;
	private PreparedStatement listQStatement;

	private PreparedStatement insertDictStatement;

	private PreparedStatement insertQuadStatement;
	private PreparedStatement getQuadStatement;
	private PreparedStatement deleteQuadStatement;
	private PreparedStatement getQuadByContextStatement;
	// private PreparedStatement replaceQuadContextStatement;

	private PreparedStatement insertQualityStatement;
	private PreparedStatement getQualityStatement;
	private PreparedStatement deleteQualityStatement;
	private PreparedStatement incrementQualityStatement;

	private StringDictionary dict;
	private int dictionaryFlushThreshold = Integer.MAX_VALUE;

	private Map<String, String> redirections = new HashMap<String, String>();

	private QualityMeasurement quality = new UniformQualityMeasurement();

	private long size;

	private IntSet usedSources = new IntRBTreeSet();
	private Timer sourceRetrievalTimer = new Timer();

	/**
	 * Gets all login info from the .password file.
	 */
	public Database() {
		try {
			Properties p = new Properties();
			InputStream in = getClass().getResourceAsStream(".password");
			p.load(in);
			in.close();

			host = p.getProperty("host");
			database = p.getProperty("database");
			user = p.getProperty("user");
			password = p.getProperty("password");

			if (user == null || password == null) {
				throw new DatabaseException(
						"Password file needs a user and a password entry.");
			}
		} catch (IOException e) {
			throw new DatabaseException("Missing password file.", e);
		}
	}

	public Database(String host, String database, String user, String password) {
		this.host = host;
		this.database = database;
		this.user = user;
		this.password = password;
	}

	/**
	 * Returns a new {@code Database} object with the same host, user and
	 * password. The new object is not connected.
	 */
	public Database clone() {
		return new Database(host, database, user, password);
	}

	public void connect() {
		if (connection != null) {
			return;
		}

		try {
			Class.forName(DRIVER);

			connection = DriverManager.getConnection("jdbc:mysql://" + host
					+ "/" + database
					+ "?useUnicode=true&characterEncoding=utf-8", user,
					password);

			// Create tables
			Statement stmt = connection.createStatement();
			stmt.execute("CREATE TABLE IF NOT EXISTS QTable (id INT PRIMARY KEY AUTO_INCREMENT, history BLOB, action BLOB, future BLOB, q DOUBLE, updateCount INT, INDEX (history(254), action(254), future(254)), INDEX (history(254))) DEFAULT CHARSET=utf8");
			stmt.execute("CREATE TABLE IF NOT EXISTS DictionaryTable (id INT PRIMARY KEY, value TEXT, INDEX (value(254))) DEFAULT CHARSET=utf8");
			stmt.execute("CREATE TABLE IF NOT EXISTS SourceIndexTable (id INT PRIMARY KEY AUTO_INCREMENT, subject INT, predicate INT, object INT, context INT, INDEX (context)) DEFAULT CHARSET=utf8");
			stmt.execute("CREATE TABLE IF NOT EXISTS QualityTable (id INT PRIMARY KEY, quality DOUBLE)");

			insertQStatement = connection
					.prepareStatement("INSERT INTO QTable (history, action, future, q, updateCount) VALUES (?, ?, ?, ?, ?)");
			updateQStatement = connection
					.prepareStatement("UPDATE QTable SET q = ?, updateCount = ? WHERE history = ? AND action = ? AND future = ?");
			clearQStatement = connection.prepareStatement("DELETE FROM QTable");
			getQStatement = connection
					.prepareStatement("SELECT q, updateCount FROM QTable WHERE history = ? AND action = ? AND future = ?");
			getBestActionQStatement = connection
					.prepareStatement("SELECT q FROM QTable WHERE history = ? ORDER BY q DESC LIMIT 1");
			listQStatement = connection
					.prepareStatement("SELECT history, action, future, q FROM QTable");

			insertDictStatement = connection
					.prepareStatement("INSERT IGNORE INTO DictionaryTable VALUES (?, ?)");

			insertQuadStatement = connection
					.prepareStatement("INSERT INTO SourceIndexTable (subject, predicate, object, context) VALUES (?, ?, ?, ?)");
			getQuadStatement = connection
					.prepareStatement("SELECT * FROM SourceIndexTable WHERE subject = ? AND predicate = ? AND object = ? and context = ?");
			deleteQuadStatement = connection
					.prepareStatement("DELETE FROM SourceIndexTable WHERE subject = ? AND predicate = ? AND object = ? AND context = ?");
			getQuadByContextStatement = connection
					.prepareStatement("SELECT subject, predicate, object, context FROM SourceIndexTable WHERE context = ?");
			// replaceQuadContextStatement = connection
			// .prepareStatement("UPDATE SourceIndexTable SET context = ? WHERE context = ?");

			insertQualityStatement = connection
					.prepareStatement("INSERT IGNORE INTO QualityTable VALUES (?, ?)");
			getQualityStatement = connection
					.prepareStatement("SELECT quality FROM QualityTable WHERE id = ?");
			deleteQualityStatement = connection
					.prepareStatement("DELETE FROM QualityTable WHERE id = ?");
			incrementQualityStatement = connection
					.prepareStatement("UPDATE QualityTable SET quality = quality + ? WHERE id = ?");

			final ResultSet entries = stmt
					.executeQuery("SELECT id, value FROM DictionaryTable");

			dict = new StringDictionary(this);

			dict.load(new AbstractIterator<Entry>() {
				@Override
				protected Entry computeNext() {
					try {
						if (entries.next()) {
							return dict.new Entry(entries.getInt(1), entries
									.getString(2));
						} else {
							return endOfData();
						}
					} catch (SQLException e) {
						Logger.getLogger(getClass().getName()).log(Level.WARN,
								"Couldn't load dictionary", e);

						return endOfData();
					}
				}
			});

			entries.close();
			stmt.close();
			
			sourceRetrievalTimer.startPaused();
		} catch (ClassNotFoundException e) {
			throw new DatabaseException("Could not find driver: " + DRIVER, e);
		} catch (SQLException e) {
			throw new DatabaseException("Could not connect to db: " + host, e);
		}
	}

	/**
	 * Closes connection to db and saves metrics.
	 */
	public void close() {
		if (connection == null) {
			return;
		}

		handleRedirections();

		flushDictionary();
		dict = null;

		try {
			connection.close();
			connection = null;
		} catch (SQLException e) {
			throw new DatabaseException("Could not close database connection",
					e);
		}

		sourceRetrievalTimer.stop();
		
		Metrics m = Metrics.getInstance();
		m.setParameter("Index memory size (bytes)", (int) size);
		m.setParameter("Sources used", usedSources.size());
		m.saveTimer("Source retrieval time", sourceRetrievalTimer);

		usedSources.clear();
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
		int sizeIncrement = 0;
		int staticSizeIncrement = 0;

		for (State s : q.getHistory()) {
			sizeIncrement += sizeMap[s.getProperties().length];
			sizeIncrement += sizeMap[s.getTypes().length];

			staticSizeIncrement = s.getProperties().length;
			staticSizeIncrement = s.getTypes().length;
		}

		for (State s : q.getFuture()) {
			sizeIncrement += sizeMap[s.getProperties().length];
			sizeIncrement += sizeMap[s.getTypes().length];

			staticSizeIncrement = s.getProperties().length;
			staticSizeIncrement = s.getTypes().length;
		}

		sizeIncrement += 24 * staticSizeIncrement;

		updateQ(q.getHistory().getBytes(), q.getAction().getBytes(dict), q
				.getFuture().getBytes(), q.getQ(), sizeIncrement);
	}

	public void updateQ(byte[] history, byte[] action, byte[] future, double q,
			int sizeIncrement) {
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
					throw new DatabaseException(
							"Updated a wrong number of rows: " + updateCount
									+ " (should be: ");
				}
			} else {
				insertQStatement.setBytes(1, history);
				insertQStatement.setBytes(2, action);
				insertQStatement.setBytes(3, future);
				insertQStatement.setDouble(4, q);
				insertQStatement.setInt(5, 0);

				insertQStatement.execute();

				size += sizeIncrement;
			}

			result.close();
		} catch (SQLException e) {
			throw new DatabaseException("Could not update Q values.", e);
		}
	}

	public boolean getQ(QValue q) {
		double newQ = getQ(q.getHistory().getBytes(),
				q.getAction().getBytes(dict), q.getFuture().getBytes());

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
		return getBestQ(history.getBytes());
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
	 * Warning: The result iterator must be completely traversed, otherwise
	 * there will be a resource leak.
	 * 
	 * @return
	 */
	public Iterator<QValue> listAllQs() {
		try {
			final ResultSet result = listQStatement.executeQuery();

			return new AbstractIterator<QValue>() {
				@Override
				protected QValue computeNext() {
					try {
						if (result.next()) {
							return new QValue(
									new StateChain(new ByteArrayInputStream(
											result.getBytes(1))),
									new Action(result.getBlob(2)
											.getBinaryStream()),
									new StateChain(new ByteArrayInputStream(
											result.getBytes(3))),
									result.getDouble(4));
						} else {
							result.close();
							return endOfData();
						}
					} catch (SQLException e) {
						endOfData();

						try {
							result.close();
						} catch (SQLException e1) {
							throw new DatabaseException(
									"Could not close result set.", e);
						}

						throw new DatabaseException(
								"Could not retrieve next q.", e);
					}
				}
			};
		} catch (SQLException e) {
			throw new DatabaseException("Could not list qs.", e);
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
		if (connection == null) {
			throw new IllegalStateException("Database is not connected.");
		}

		return dict;
	}

	public synchronized void addQuad(Quad q) {
		addQuad(q.getGraph(), q.getSubject(), q.getPredicate(), q.getObject());
	}

	public synchronized void addQuad(Node g, Node s, Node p, Node o) {
		addQuad(dict.getId(g.toString()), dict.getId(s.toString()),
				dict.getId(p.toString()), dict.getId(o.toString()));
	}

	public synchronized void addQuad(int g, int s, int p, int o) {
		try {
			getQuadStatement.setInt(1, s);
			getQuadStatement.setInt(2, p);
			getQuadStatement.setInt(3, o);
			getQuadStatement.setInt(4, g);

			ResultSet result = getQuadStatement.executeQuery();

			if (!result.next()) {
				insertQuadStatement.setInt(1, s);
				insertQuadStatement.setInt(2, p);
				insertQuadStatement.setInt(3, o);
				insertQuadStatement.setInt(4, g);

				insertQuadStatement.execute();

				insertQuality(g);
			}

			result.close();
		} catch (SQLException e) {
			throw new DatabaseException("Could not insert quad.", e);
		}
	}

	public void deleteQuad(Quad q) {
		try {
			deleteQuadStatement
					.setInt(1, dict.getId(q.getSubject().toString()));
			deleteQuadStatement.setInt(2,
					dict.getId(q.getPredicate().toString()));
			deleteQuadStatement.setInt(3, dict.getId(q.getObject().toString()));
			deleteQuadStatement.setInt(4, dict.getId(q.getGraph().toString()));

			deleteQuadStatement.execute();
		} catch (SQLException e) {
			throw new DatabaseException("Could not insert quad.", e);
		}
	}

	public synchronized Iterator<Quad> findAllByURI(String uri) {
		Collection<Quad> result = new ArrayList<Quad>();

		try {
			sourceRetrievalTimer.unpause();
			
			int uriId = dict.getId(uri);
			usedSources.add(uriId);

			getQuadByContextStatement.setInt(1, uriId);
			ResultSet quads = getQuadByContextStatement.executeQuery();

			while (quads.next()) {
				result.add(new Quad(Node.createURI(dict.getString(quads
						.getInt(4))), Node.createURI(dict.getString(quads
						.getInt(1))), Node.createURI(dict.getString(quads
						.getInt(2))), Node.createURI(dict.getString(quads
						.getInt(3)))));
			}

			quads.close();
			sourceRetrievalTimer.pause();
		} catch (SQLException e) {
			throw new DatabaseException("Could not receive quads.", e);
		}

		return result.iterator();
	}

	public synchronized void addRedirect(String from, String to) {
		while (redirections.containsKey(from)) {
			from = redirections.get(from);
		}

		redirections.put(to, from);
	}

	public synchronized void handleRedirections() {
		try {
			Iterator<String> toIter = redirections.keySet().iterator();

			while (toIter.hasNext()) {
				String to = toIter.next();
				String from = redirections.get(to);
				int fromId = dict.getId(from);

				getQuadByContextStatement.setInt(1, dict.getId(to));
				ResultSet quads = getQuadByContextStatement.executeQuery();

				while (quads.next()) {
					addQuad(fromId, quads.getInt(1), quads.getInt(2),
							quads.getInt(3));
				}
			}

			redirections.clear();
		} catch (SQLException e) {
			throw new DatabaseException("Could not update URIs.", e);
		}
	}

	public void setDictionaryFlushThreshold(int dictionaryFlushThreshold) {
		this.dictionaryFlushThreshold = dictionaryFlushThreshold;
	}

	private void insertQuality(int id) {
		try {
			insertQualityStatement.setInt(1, id);
			insertQualityStatement.setDouble(2, quality.getQuality(id));

			insertQualityStatement.execute();
		} catch (SQLException e) {
			throw new DatabaseException("Could not insert quality.", e);
		}
	}

	public double getQuality(int id) {
		try {
			getQualityStatement.setInt(1, id);

			ResultSet queryResult = getQualityStatement.executeQuery();
			double result = 0;

			if (queryResult.next()) {
				result = queryResult.getDouble(1);
				queryResult.close();
			} else {
				queryResult.close();
				insertQuality(id);

				return getQuality(id);
			}

			return result;
		} catch (SQLException e) {
			throw new DatabaseException("Could not get quality.", e);
		}
	}

	public double getQuality(Node n) {
		return getQuality(dict.getId(n));
	}

	public double getQuality(String uri) {
		return getQuality(dict.getId(uri));
	}

	public void deleteQuality(Node n) {
		deleteQuality(dict.getId(n));
	}

	public void deleteQuality(int id) {
		try {
			deleteQualityStatement.setInt(1, id);
			deleteQualityStatement.execute();
		} catch (SQLException e) {
			throw new DatabaseException("Could not delete quality.", e);
		}
	}

	public void dictionaryIdAdded() {
		if (dict.getNewIdAmount() >= dictionaryFlushThreshold) {
			flushDictionary();
			Logger.getLogger(getClass()).log(Level.DEBUG,
					"Flushing dictionary. Size: " + dict.size());
		}
	}

	public void setQualityMeasurement(QualityMeasurement qualityMeasurement) {
		quality = qualityMeasurement;
	}

	public void incrementQuality(String url, double increment) {
		incrementQuality(dict.getId(url), increment);
	}

	public void incrementQuality(int id, double increment) {
		try {
			getQualityStatement.setInt(1, id);
			ResultSet queryResult = getQualityStatement.executeQuery();

			if (queryResult.next()) {
				incrementQualityStatement.setDouble(1, increment);
				incrementQualityStatement.setInt(2, id);
				incrementQualityStatement.execute();
			} else {
				insertQualityStatement.setInt(1, id);
				insertQualityStatement.setDouble(2, quality.getQuality(id)
						+ increment);

				insertQualityStatement.execute();
			}

			queryResult.close();
		} catch (SQLException e) {
			throw new DatabaseException("Could not increment Quality.", e);
		}
	}

	public QualityMeasurement getQualityMeasurement() {
		return quality;
	}
}
