package org.apache.ibatis.session.defaults;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;

public class DefaultSqlSessionFactory implements SqlSessionFactory {

	private final Configuration configuration;

	//构造器
	public DefaultSqlSessionFactory(Configuration configuration) {
		this.configuration = configuration;
	}

	//获取会话
	public SqlSession openSession() {
		return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
	}

	//获取会话(是否自动提交)
	public SqlSession openSession(boolean autoCommit) {
		return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, autoCommit);
	}

	//获取会话(执行类型)
	public SqlSession openSession(ExecutorType execType) {
		return openSessionFromDataSource(execType, null, false);
	}

	//获取会话(事务隔离级别)
	public SqlSession openSession(TransactionIsolationLevel level) {
		return openSessionFromDataSource(configuration.getDefaultExecutorType(), level, false);
	}

	//获取会话(执行类型，事务隔离级别)
	public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
		return openSessionFromDataSource(execType, level, false);
	}

	//获取会话(执行类型，是否自动提交)
	public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
		return openSessionFromDataSource(execType, null, autoCommit);
	}

	//获取会话(数据库连接)
	public SqlSession openSession(Connection connection) {
		return openSessionFromConnection(configuration.getDefaultExecutorType(), connection);
	}

	//获取会话(执行类型，数据库连接)
	public SqlSession openSession(ExecutorType execType, Connection connection) {
		return openSessionFromConnection(execType, connection);
	}

	//获取配置信息
	public Configuration getConfiguration() {
		return configuration;
	}

	//从数据源中获取会话
	private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
		Transaction tx = null;
		try {
			//获取运行环境
			final Environment environment = configuration.getEnvironment();
			//获取事务工厂
			final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
			//获取事务
			tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
			//获取执行器
			final Executor executor = configuration.newExecutor(tx, execType);
			//返回默认会话
			return new DefaultSqlSession(configuration, executor, autoCommit);
		} catch (Exception e) {
			closeTransaction(tx);
			throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
		} finally {
			ErrorContext.instance().reset();
		}
	}

	//从数据库连接中获取会话
	private SqlSession openSessionFromConnection(ExecutorType execType, Connection connection) {
		try {
			boolean autoCommit;
			try {
				//设置自动提交
				autoCommit = connection.getAutoCommit();
			} catch (SQLException e) {
				autoCommit = true;
			}
			//获取运行环境
			final Environment environment = configuration.getEnvironment();
			//获取事务工厂
			final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
			//获取事务
			final Transaction tx = transactionFactory.newTransaction(connection);
			//获取执行器
			final Executor executor = configuration.newExecutor(tx, execType);
			//返回默认会话
			return new DefaultSqlSession(configuration, executor, autoCommit);
		} catch (Exception e) {
			throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
		} finally {
			ErrorContext.instance().reset();
		}
	}

	//获取事务工厂
	private TransactionFactory getTransactionFactoryFromEnvironment(Environment environment) {
		//如果没有配置事务工厂，则返回托管事务工厂
		if (environment == null || environment.getTransactionFactory() == null) {
			return new ManagedTransactionFactory();
		}
		return environment.getTransactionFactory();
	}

	//关闭事务
	private void closeTransaction(Transaction tx) {
		if (tx != null) {
			try {
				tx.close();
			} catch (SQLException ignore) {
			}
		}
	}

}
