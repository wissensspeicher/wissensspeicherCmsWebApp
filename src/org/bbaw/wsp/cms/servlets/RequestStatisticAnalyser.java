package org.bbaw.wsp.cms.servlets;

/**
 * Class is used to split an incoming Request and match it with the Database
 * 
 * @author shk2
 * 
 */
public class RequestStatisticAnalyser extends Tablenames {
    private final String request;

    public static void main(String[] args) {
	MySqlConnector con = new MySqlConnector("localhost", "3306",
		"WspCmsCore");

	try {
	    con.readDataBase();
	    con.inserSingelElementToTable(QUERIES, "query", "Marx Mega");

	    System.out.println(con.getID(RELEVANT_DOCS, "Http://Bla und so"));
	    con.updateQueries("Marx");
	    con.closeConnection();

	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public RequestStatisticAnalyser(String request) {
	this.request = request;
    }
}
