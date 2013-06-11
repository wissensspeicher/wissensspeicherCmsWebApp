package org.bbaw.wsp.cms.servlets;

/**
 * Class is used to split an
 * 
 * @author shk2
 * 
 */
public class RequestStatisticAnalyser {
	private final String request;

	public static void main(String[] args) {
		MySqlConnector con = new MySqlConnector();
		try {
			con.readDataBase();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public RequestStatisticAnalyser(String request) {
		this.request = request;
	}

	/**
	 * Methode creates connection to the MySql Database.
	 * 
	 */
	private void getDBConnection() {
		// gehe rein tue nix
	}

}
