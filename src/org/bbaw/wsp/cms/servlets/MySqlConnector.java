package org.bbaw.wsp.cms.servlets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class MySqlConnector extends Tablenames {
    private Connection connect = null;

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

	value = createValidSQLString(value);

	Statement statement = connect.createStatement();
	ResultSet resultSet = null;
	PreparedStatement preparedStatement = null;

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

    /**
     * 
     * @param table
     * @param value
     * @return
     * @throws SQLException
     */
    public String getID(String table, String value) throws SQLException {

	value = createValidSQLString(value);

	Statement statement = connect.createStatement();
	ResultSet resultSet = null;

	String element = "";

	switch (table) {
	case QUERY_WORDS:
	    element = QUERY_WORDS_COL;
	    break;
	case QUERIES:
	    element = QUERIES_COL;
	    break;
	case RELEVANT_COUNTER:
	    element = RELEVANT_COUNTER_COL;
	    break;
	case RELEVANT_DOCS:
	    element = RELEVANT_DOCS_COL;
	    break;
	default:
	    return null;

	}

	String temp = null;
	resultSet = statement.executeQuery("select id from " + table
		+ " where " + element + " = " + value + ";");
	while (resultSet.next()) {
	    temp = resultSet.getString(1);
	}
	resultSet.close();
	return temp;

    }

    public void updateQueries(String value) throws SQLException {

	String bla = "select " + QUERIES_COL + " from " + QUERIES + ";";

	Statement st = connect.createStatement();

	ResultSet resultSet = st.executeQuery(bla);

	int i = 0;
	String id = getID(QUERY_WORDS, value);
	if (id == null)
	    return;

	while (resultSet.next()) {
	    String temp = resultSet.getString(++i);
	    if (temp.startsWith(value)) {
		updateSingelValueInTable(QUERIES, "id_queryWords", QUERIES_COL,
			id, createValidSQLString(temp));
	    }
	}

    }

    public void updateSingelValueInTable(String table, String columnToUpdate,
	    String columnConditionToUpdate, String value, String conditionValue)
	    throws SQLException {

	PreparedStatement preparedStatement = null;
	preparedStatement = connect.prepareStatement("update " + table
		+ " set " + columnToUpdate + " = " + value + " where "
		+ columnConditionToUpdate + " = " + conditionValue + ";");
	preparedStatement.executeUpdate();
	System.out.println("Database updated");

    }

    private String createValidSQLString(String str) {
	return "\"" + str + "\"";
    }

    // You need to close the resultSet
    public void closeConnection() {
	try {

	    if (connect != null) {
		connect.close();
	    }
	} catch (Exception e) {

	}
    }

}
