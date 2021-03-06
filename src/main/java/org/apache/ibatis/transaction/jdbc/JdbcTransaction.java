package org.apache.ibatis.transaction.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionException;

//Jdbc事务
public class JdbcTransaction implements Transaction {

	private static final Log log = LogFactory.getLog(JdbcTransaction.class);

	protected Connection connection;              //数据库连接
	protected DataSource dataSource;              //数据源
	protected TransactionIsolationLevel level;    //事务隔离级别
	protected boolean autoCommmit;               //是否自动提交

	//构造器
	public JdbcTransaction(DataSource ds, TransactionIsolationLevel desiredLevel, boolean desiredAutoCommit) {
		dataSource = ds;
		level = desiredLevel;
		autoCommmit = desiredAutoCommit;
	}

	//构造器
	public JdbcTransaction(Connection connection) {
		this.connection = connection;
	}

	//获取数据库连接
	public Connection getConnection() throws SQLException {
		if (connection == null) {
			openConnection();
		}
		return connection;
	}

	//提交事务
	public void commit() throws SQLException {
		if (connection != null && !connection.getAutoCommit()) {
			if (log.isDebugEnabled()) {
				log.debug("Committing JDBC Connection [" + connection + "]");
			}
			connection.commit();
		}
	}

	//回滚事务
	public void rollback() throws SQLException {
		if (connection != null && !connection.getAutoCommit()) {
			if (log.isDebugEnabled()) {
				log.debug("Rolling back JDBC Connection [" + connection + "]");
			}
			connection.rollback();
		}
	}

	//关闭连接
	public void close() throws SQLException {
		if (connection != null) {
			resetAutoCommit();
			if (log.isDebugEnabled()) {
				log.debug("Closing JDBC Connection [" + connection + "]");
			}
			connection.close();
		}
	}

	protected void setDesiredAutoCommit(boolean desiredAutoCommit) {
		try {
			// 和原来的比一下，再设置autocommit，是考虑多次重复设置的性能问题？
			if (connection.getAutoCommit() != desiredAutoCommit) {
				if (log.isDebugEnabled()) {
					log.debug(
							"Setting autocommit to " + desiredAutoCommit + " on JDBC Connection [" + connection + "]");
				}
				connection.setAutoCommit(desiredAutoCommit);
			}
		} catch (SQLException e) {
			throw new TransactionException("Error configuring AutoCommit.  "
					+ "Your driver may not support getAutoCommit() or setAutoCommit(). " + "Requested setting: "
					+ desiredAutoCommit + ".  Cause: " + e, e);
		}
	}

	// 见下面注释，貌似是说是对有些DB的一个workaround
	protected void resetAutoCommit() {
		try {
			if (!connection.getAutoCommit()) {
				// MyBatis does not call commit/rollback on a connection if just selects were
				// performed.
				// Some databases start transactions with select statements
				// and they mandate a commit/rollback before closing the connection.
				// A workaround is setting the autocommit to true before closing the connection.
				// Sybase throws an exception here.
				if (log.isDebugEnabled()) {
					log.debug("Resetting autocommit to true on JDBC Connection [" + connection + "]");
				}
				connection.setAutoCommit(true);
			}
		} catch (SQLException e) {
			log.debug("Error resetting autocommit to true " + "before closing the connection.  Cause: " + e);
		}
	}

	//打开数据库连接
	protected void openConnection() throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug("Opening JDBC Connection");
		}
		connection = dataSource.getConnection();
		if (level != null) {
			connection.setTransactionIsolation(level.getLevel());
		}
		setDesiredAutoCommit(autoCommmit);
	}

}
