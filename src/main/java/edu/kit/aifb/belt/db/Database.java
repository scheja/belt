package edu.kit.aifb.belt.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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
	private PreparedStatement createQStatement;
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
		try {
			Class.forName(DRIVER);
			
			connection = DriverManager.getConnection("jdbc:mysql://" + host, user, password);
			createQStatement = connection.prepareStatement("INSERT INTO belt (history, action, future, q, updateCount) VALUES (?, ?, ?, ?, ?)");
			updateQStatement = connection.prepareStatement("UPDATE belt SET q = ?, updateCount = ? WHERE history = ? AND action = ? AND future = ?");
			getQStatement = connection.prepareStatement("SELECT q, updateCount FROM belt WHERE history = ? AND action = ? AND future = ?");
		} catch (ClassNotFoundException e) {
			throw new DatabaseException("Could not find driver: " + DRIVER, e);
		} catch (SQLException e) {
			throw new DatabaseException("Could not connect to db: " + host, e);
		}
	}
}