package org.apache.ibatis.transaction.managed;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;

/**
 * 托管事务工厂,  默认 情况下它会关闭连接。 然而一些容器并不希望这样, 因此如果你需要从连接中停止 它,将 closeConnection 属性设置为
 * false。
 */
public class ManagedTransactionFactory implements TransactionFactory {

	private boolean closeConnection = true;

	public void setProperties(Properties props) {
		// 设置closeConnection属性
		if (props != null) {
			String closeConnectionProperty = props.getProperty("closeConnection");
			if (closeConnectionProperty != null) {
				closeConnection = Boolean.valueOf(closeConnectionProperty);
			}
		}
	}

	public Transaction newTransaction(Connection conn) {
		return new ManagedTransaction(conn, closeConnection);
	}

	public Transaction newTransaction(DataSource ds, TransactionIsolationLevel level, boolean autoCommit) {
		// Silently ignores autocommit and isolation level, as managed transactions are
		// entirely
		// controlled by an external manager. It's silently ignored so that
		// code remains portable between managed and unmanaged configurations.
		return new ManagedTransaction(ds, level, closeConnection);
	}
}
