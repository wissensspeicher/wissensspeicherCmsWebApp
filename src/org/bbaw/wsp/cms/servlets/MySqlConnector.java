package org.bbaw.wsp.cms.servlets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySqlConnector {
	private Connection connect = null;
	private Statement statement = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;

	public void readDataBase() throws Exception {

		try {
			// This will load the MySQL driver, each DB has its own driver
			Class.forName("com.mysql.jdbc.Driver");
			// Setup the connection with the DB
			connect = DriverManager.getConnection("jdbc:mysql://localhost/dbreq?"
					+ "user=root&password=toor");

			// Statements allow to issue SQL queries to the database
			statement = connect.createStatement();
			// Result set get the result of the SQL query

			resultSet = statement.executeQuery("select * from dbreq");
			System.out.println(resultSet);

		} catch (Exception e) {
			throw e;
		} finally {
			close();
		}

	}

	/**
	 * Methode contains helps to add or upate a request of the MySQL Database
	 * 
	 * @param request
	 * @throws SQLException
	 */
	public void inserStatement(String request) throws SQLException {
		preparedStatement = connect
				.prepareStatement("select REQUEST from dbreq.COMMENTS "
						+ "WHERE REQUEST = " + request);
		preparedStatement.executeQuery();

		if (!resultSet.getString("REQUEST").equals(request)) {
			preparedStatement = connect
					.prepareStatement("insert into dbreq.COMMENTS values(" + request
							+ ",0,0)");
			preparedStatement.executeUpdate();
		} else {

			preparedStatement = connect
					.prepareStatement("UPDATE HITS SET HITS=HITS+1 from dbreq.COMMENTS WHERE REQUEST = "
							+ request);

			preparedStatement.executeUpdate();
		}

	}

	private void writeMetaData(ResultSet resultSet) throws SQLException {
		// Now get some metadata from the database
		// Result set get the result of the SQL query

		System.out.println("The columns in the table are: ");

		System.out.println("Table: " + resultSet.getMetaData().getTableName(1));
		for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
			System.out.println("Column " + i + " "
					+ resultSet.getMetaData().getColumnName(i));
		}
	}

	// You need to close the resultSet
	private void close() {
		try {
			if (resultSet != null) {
				resultSet.close();
			}

			if (statement != null) {
				statement.close();
			}

			if (connect != null) {
				connect.close();
			}
		} catch (Exception e) {

		}
	}

}
