package net.zyuiop.crosspermissions.api.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author zyuiop
 */
public class BasicSQLDatabase implements SQLDatabase {
	private final String host;
	private final int port;
	private final String database;
	private final String userName;
	private final String password;

	public BasicSQLDatabase(String host, int port, String database, String userName, String password) {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		this.host = host;
		this.port = port;
		this.database = database;
		this.userName = userName;
		this.password = password;
	}

	@Override
	public Connection getConnection() throws SQLException {
		Properties connectionProps = new Properties();
		connectionProps.put("user", userName);
		connectionProps.put("password", password);
		connectionProps.put("useColumnNamesInFindColumn", true);

		return DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, connectionProps);
	}

	@Override
	public void execute(SQLOperationExecutor operationExecutor) {
		Connection con = null;
		try {
			con = getConnection();
			operationExecutor.execute(con);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (con != null) try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public <T> T query(SQLOperationReturn<T> operationReturn) {
		Connection con = null;
		T result = null;
		try {
			con = getConnection();
			result = operationReturn.execute(con);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (con != null) try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
}
