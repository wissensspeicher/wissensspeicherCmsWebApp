package org.bbaw.wsp.cms.servlets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class MySqlConnector {
    private Connection connect = null;
    private Statement statement = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;
    private final String server;
    private final String port;
    private final String database;

    /**
     * 
     * @param server
     * @param port
     * @param database
     */
    public MySqlConnector(final String server, final String port,
	    final String database) {
	this.server = server;
	this.port = port;
	this.database = database;

    }

    public void readDataBase() throws Exception {

	try {
	    // This will load the MySQL driver, each DB has its own driver
	    Class.forName("com.mysql.jdbc.Driver");
	    // Setup the connection with the DB
	    connect = getConnection();

	    // Statements allow to issue SQL queries to the database
	    statement = connect.createStatement();
	    // Result set get the result of the SQL query

	    // resultSet = statement.executeQuery("select * from ");
	    // System.out.println(resultSet);

	} catch (Exception e) {
	    throw e;
	}

    }

    private Connection getConnection() throws SQLException {

	Connection conn = null;
	Properties connectionProps = new Properties();
	connectionProps.put("user", "root");
	connectionProps.put("password", "toor");

	conn = DriverManager.getConnection("jdbc:" + "mysql" + "://" + server
		+ ":" + port + "/" + database, connectionProps);

	System.out.println("Connected to database");
	return conn;
    }

    /**
     * 
     * @param table
     * @param element
     *            (s)
     * @param value
     *            (s)
     * @throws SQLException
     */
    public void inserSingelElementToTable(String table, String element,
	    String value) throws SQLException {

	if (table.equals("Queries")) {
	    resultSet = statement
		    .executeQuery("select requests from Queries where query="
			    + value);

	    int temp = 0;
	    while (resultSet.next()) {
		temp = Integer.parseInt(resultSet.getString(1));
	    }

	    if (temp > 0) {
		updateSingelValueInTable(table, "requests", element, ""
			+ ++temp, value);
		resultSet.close();
		return;
	    }
	    resultSet.close();
	}

	preparedStatement = connect.prepareStatement("insert into " + table
		+ " (" + element + ",requests) " + " values " + "(" + value
		+ ",1);");
	preparedStatement.executeUpdate();
	System.out.println("Database updated");

    }

    public void updateSingelValueInTable(String table, String columnToUpdate,
	    String columnConditionToUpdate, String value, String conditionValue)
	    throws SQLException {
	preparedStatement = connect.prepareStatement("update " + table
		+ " set " + columnToUpdate + " = " + value + " where "
		+ columnConditionToUpdate + " = " + conditionValue + ";");
	preparedStatement.executeUpdate();
	System.out.println("Database updated");

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
    public void closeConnection() {
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
