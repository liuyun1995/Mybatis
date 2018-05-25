package org.apache.ibatis.transaction.managed;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;

//托管事务
public class ManagedTransaction implements Transaction {

	private static final Log log = LogFactory.getLog(ManagedTransaction.class);

	private DataSource dataSource;                //数据库源
	private TransactionIsolationLevel level;      //事务隔离级别
	private Connection connection;                //数据库连接
	private boolean closeConnection;             //是否关闭连接

	//构造器
	public ManagedTransaction(Connection connection, boolean closeConnection) {
		this.connection = connection;
		this.closeConnection = closeConnection;
	}

	//构造器
	public ManagedTransaction(DataSource ds, TransactionIsolationLevel level, boolean closeConnection) {
		this.dataSource = ds;
		this.level = level;
		this.closeConnection = closeConnection;
	}

	//获取数据库连接
	public Connection getConnection() throws SQLException {
		if (this.connection == null) {
			openConnection();
		}
		return this.connection;
	}

	//提交事务
	public void commit() throws SQLException {
		// Does nothing
	}

	//回滚事务
	public void rollback() throws SQLException {
		// Does nothing
	}

	//关闭连接
	public void close() throws SQLException {
		//如果配置文件配置了closeConnection=false, 则不关闭连接
		if (this.closeConnection && this.connection != null) {
			if (log.isDebugEnabled()) {
				log.debug("Closing JDBC Connection [" + this.connection + "]");
			}
			this.connection.close();
		}
	}

	//打开连接
	protected void openConnection() throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug("Opening JDBC Connection");
		}
		this.connection = this.dataSource.getConnection();
		if (this.level != null) {
			this.connection.setTransactionIsolation(this.level.getLevel());
		}
	}

}
