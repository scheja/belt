package edu.kit.aifb.belt.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Properties;
import java.sql.PreparedStatement;

/**
 * Connects to the database, provides standard functionality.
 * 
 * @author sibbo
 */
public class Database {
	private static final String DRIVER = "com.mysql.jdbc.Driver";

	private String host;
	private String user;
	private String password;

	private Connection connection;
	private PreparedStatement insertQStatement;
	private PreparedStatement updateQStatement;
	private PreparedStatement getQStatement;

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

	public Database(String host, String database, String user, String password) {
		this.host = host;
		this.user = user;
		this.password = password;
	}

	public void connect() {
		if (connection != null) {
			return;
		}

		try {
			Class.forName(DRIVER);

			connection = DriverManager.getConnection("jdbc:mysql://" + host, user, password);
			insertQStatement = connection
					.prepareStatement("INSERT INTO QTable (history, action, future, q, updateCount) VALUES (?, ?, ?, ?, ?)");
			updateQStatement = connection
					.prepareStatement("UPDATE QTable SET q = ?, updateCount = ? WHERE history = ? AND action = ? AND future = ?");
			getQStatement = connection
					.prepareStatement("SELECT q, updateCount FROM QTable WHERE history = ? AND action = ? AND future = ?");
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

		try {
			connection.close();
			connection = null;
		} catch (SQLException e) {
			throw new DatabaseException("Could not close database connection", e);
		}
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
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			throw new DatabaseException("Error while fetching q value.", e);
		}
	}
}