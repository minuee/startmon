package startMon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class MysqlConnect {
	private Connection conn;
	private Statement statement;
	private ResultSet resultSet;
	private static MysqlConnect db;
	
	// DEMO TEST VARIABLE
	private Connection conn2;
	private Statement statement2;

	private MysqlConnect() {
		String driver = "com.mysql.jdbc.Driver";
		
		// Master
    	String url = "jdbc:mysql://121.78.239.245/";
		String dbName = "monitor";
		String userName = "jsm";
		String password = "tlfkthsl";
		
		try {
			Class.forName(driver).newInstance();
			this.conn = (Connection) DriverManager.getConnection(url + dbName, userName, password);

			// 테스트일 경우 Slave DB 추가 연결
			if (StartMonitor.DEMO_TEST) {
				// Slave
				String url2 = "jdbc:mysql://1.201.145.35/";
				String dbName2 = "monitor";
				String userName2 = "jsm";
				String password2 = "tlfkthsl";
				this.conn2 = (Connection) DriverManager.getConnection(url2 + dbName2, userName2, password2);
			}
		} catch (Exception sqle) {
			sqle.printStackTrace();
		}
	}

	public static synchronized MysqlConnect getConn() {
		if (db == null) {
			db = new MysqlConnect();
		}

		return db;
	}

	public ResultSet query(String query) throws SQLException {
		statement = conn.createStatement();
		resultSet = statement.executeQuery(query);
		return resultSet;
	}

	public int insert(String insertQuery) throws SQLException {
		int result;

		// 테스트일 경우 Slave DB에 삽입
		if (StartMonitor.DEMO_TEST) {
			statement2 = conn2.createStatement();
			result = statement2.executeUpdate(insertQuery);
		} else { 
			statement = conn.createStatement();
			result = statement.executeUpdate(insertQuery);
		}
		
		return result;
	}

	public int update(String updateQuery) throws SQLException {
		statement = conn.createStatement();
		int result = statement.executeUpdate(updateQuery);
		return result;
	}

	public void close() {
		try {
			if (resultSet != null) {
				resultSet.close();
			}

			if (statement != null) {
				statement.close();
			}

			if (conn != null) {
				conn.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}