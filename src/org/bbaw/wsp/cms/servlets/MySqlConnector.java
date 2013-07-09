package org.bbaw.wsp.cms.servlets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

public class MySqlConnector extends Tablenames {
    private static Connection connect = null;

    private static String server;
    private static String port;
    private static String database;
    private static MySqlConnector con;
    private static Boolean hasInfos = false;

    /**
     * 
     * @param server
     * @param port
     * @param database
     */
    private MySqlConnector() {

    }

    public void setValues(final String server, final String port,
	    final String database) {
	this.server = server;
	this.port = port;
	this.database = database;
    }

    public static MySqlConnector getInstance() throws Exception {
	if (con == null) {
	    con = new MySqlConnector();

	}
	return con;
    }

    public static void readDataBase() throws Exception {

	try {
	    // This will load the MySQL driver, each DB has its own driver
	    Class.forName("com.mysql.jdbc.Driver");
	    // Setup the connection with the DB
	    connect = getConnection();

	    hasInfos = true;
	    // Statements allow to issue SQL queries to the database

	    // Result set get the result of the SQL query

	    // resultSet = statement.executeQuery("select * from ");
	    // System.out.println(resultSet);

	} catch (Exception e) {
	    throw e;
	}

    }

    public Boolean hasConnectionInfos() {
	return hasInfos;
    }

    private static Connection getConnection() throws SQLException {

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

	System.out.println("inserSingelElementToTable an stelle queries");
	if (table == Tablenames.QUERIES) {
	    resultSet = statement
		    .executeQuery("select requests from Queries where query="
			    + value);

	    int temp = 0;
	    while (resultSet.next()) {
		temp = Integer.parseInt(resultSet.getString(1));
	    }
	    System.out.println(temp);

	    if (temp > 0) {
		updateSingelValueInTable(table, "requests", element, ""
			+ ++temp, value);
		resultSet.close();
		return;
	    } else {
		resultSet.close();
		preparedStatement = connect.prepareStatement("insert into "
			+ table + " (" + element + ",requests) " + " values "
			+ "(" + value + ",1);");
		preparedStatement.executeUpdate();
		System.out.println("Database updated");
	    }

	}

	else if (table == Tablenames.QUERY_WORDS) {
	    System.out
		    .println("inserSingelElementToTable an stelle querywords");
	    resultSet = statement.executeQuery("select id from "
		    + Tablenames.QUERY_WORDS + " where queryWord=" + value);

	    int temp = 0;
	    while (resultSet.next()) {
		temp = Integer.parseInt(resultSet.getString(1));
	    }
	    System.out.println(temp);

	    if (!(temp > 0)) {

		preparedStatement = connect.prepareStatement("insert into "
			+ table + " (" + element + ") values (" + value + ");");
		preparedStatement.executeUpdate();
		return;
	    }
	}

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

	String stat = "select " + QUERIES_COL + " from " + QUERIES + ";";

	Statement st = connect.createStatement();

	ResultSet resultSet = st.executeQuery(stat);

	String id_qword = getID(QUERY_WORDS, value);

	if (id_qword == null)
	    return;
	value = value.toLowerCase().trim();
	// int i = 0;
	resultSet.beforeFirst();
	while (resultSet.next()) {

	    String temp = resultSet.getString(1);

	    String compareTemp = temp;
	    compareTemp = compareTemp.toLowerCase().trim();

	    System.out.println("comparetemp: " + compareTemp);
	    System.out.println("value: " + value);

	    if (compareTemp.startsWith(value) || compareTemp.equals(value)) {

		String id_q = getID(QUERIES, temp);
		System.out.println(id_q);
		System.out.println(id_qword);

		inserQueryWordConnection(id_qword, id_q);

	    }
	    System.out.println("Ende des Durchlaufes");

	}
	resultSet.close();

    }

    public void inserQueryWordConnection(String queryWord_id, String query_id)
	    throws SQLException {

	System.out.println("inserQueryWordConnection");
	queryWord_id = createValidSQLString(queryWord_id);
	query_id = createValidSQLString(query_id);

	Statement statement = connect.createStatement();
	ResultSet resultSet = null;
	PreparedStatement preparedStatement = null;

	resultSet = statement.executeQuery("select id from "
		+ QUERY_WORD_CONNECTION + " where "
		+ QUERY_WORD_CONNECTION_WORD_COL + " = " + queryWord_id
		+ " and " + QUERY_WORD_CONNECTION_QUERY_COL + " = " + query_id);

	int temp = 0;
	while (resultSet.next()) {
	    temp = Integer.parseInt(resultSet.getString(1));
	}

	if (!(temp > 0)) {

	    resultSet.close();
	    preparedStatement = connect.prepareStatement("insert into "
		    + QUERY_WORD_CONNECTION + " ("
		    + QUERY_WORD_CONNECTION_WORD_COL + ", "
		    + QUERY_WORD_CONNECTION_QUERY_COL + ") " + " values ("
		    + queryWord_id + ", " + query_id + ");");
	    preparedStatement.executeUpdate();
	    System.out.println("Database updated");
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
	preparedStatement.close();

    }

    public ArrayList<String> getQueries(String value) throws SQLException {

	value = createValidSQLString(value);

	System.out.println("getQueries");

	String request = "select " + QUERIES_COL + " from " + QUERIES
		+ " where id = " + "(select " + QUERY_WORD_CONNECTION_QUERY_COL
		+ " from " + QUERY_WORD_CONNECTION + " where "
		+ QUERY_WORD_CONNECTION_WORD_COL + " = (select id from "
		+ QUERY_WORDS + " where " + QUERY_WORDS_COL + " = " + value
		+ ")) order by requests desc;";

	System.out.println(request);

	ArrayList<String> temp = new ArrayList<String>();

	value = createValidSQLString(value);

	Statement statement = connect.createStatement();
	ResultSet resultSet = null;

	resultSet = statement.executeQuery(request);

	int i = 0;
	while (resultSet.next()) {
	    temp.add(resultSet.getString(++i));
	}

	return temp;

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
