package net.zyuiop.crosspermissions.api.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * @author zyuiop
 */
public interface SQLDatabase {
	Connection getConnection() throws SQLException;

	void execute(SQLOperationExecutor operationExecutor);

	<T> T query(SQLOperationReturn<T> operationReturn);

	interface SQLOperationExecutor {
		void execute(Connection connection) throws SQLException;
	}

	interface SQLOperationReturn<T> {
		T execute(Connection connection) throws SQLException;
	}
}
