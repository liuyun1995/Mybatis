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

	public DefaultSqlSessionFactory(Configuration configuration) {
		this.configuration = configuration;
	}

	public SqlSession openSession() {
		return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
	}

	public SqlSession openSession(boolean autoCommit) {
		return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, autoCommit);
	}

	public SqlSession openSession(ExecutorType execType) {
		return openSessionFromDataSource(execType, null, false);
	}

	public SqlSession openSession(TransactionIsolationLevel level) {
		return openSessionFromDataSource(configuration.getDefaultExecutorType(), level, false);
	}

	public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
		return openSessionFromDataSource(execType, level, false);
	}

	public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
		return openSessionFromDataSource(execType, null, autoCommit);
	}

	// 以下2个方法都会调用openSessionFromConnection
	public SqlSession openSession(Connection connection) {
		return openSessionFromConnection(configuration.getDefaultExecutorType(), connection);
	}

	public SqlSession openSession(ExecutorType execType, Connection connection) {
		return openSessionFromConnection(execType, connection);
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	// 从数据源获取会话
	private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level,
			boolean autoCommit) {
		Transaction tx = null;
		try {
			// 获取配置的环境信息
			final Environment environment = configuration.getEnvironment();
			// 获取事务工厂
			final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
			// 通过工厂生成事务
			tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
			// 生成一个执行器
			final Executor executor = configuration.newExecutor(tx, execType);
			// 生成DefaultSqlSession
			return new DefaultSqlSession(configuration, executor, autoCommit);
		} catch (Exception e) {
			// 如果打开事务出错，则关闭它
			closeTransaction(tx);
			throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
		} finally {
			// 最后清空错误上下文
			ErrorContext.instance().reset();
		}
	}

	private SqlSession openSessionFromConnection(ExecutorType execType, Connection connection) {
		try {
			boolean autoCommit;
			try {
				autoCommit = connection.getAutoCommit();
			} catch (SQLException e) {
				autoCommit = true;
			}
			final Environment environment = configuration.getEnvironment();
			final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
			final Transaction tx = transactionFactory.newTransaction(connection);
			final Executor executor = configuration.newExecutor(tx, execType);
			return new DefaultSqlSession(configuration, executor, autoCommit);
		} catch (Exception e) {
			throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
		} finally {
			ErrorContext.instance().reset();
		}
	}

	private TransactionFactory getTransactionFactoryFromEnvironment(Environment environment) {
		// 如果没有配置事务工厂，则返回托管事务工厂
		if (environment == null || environment.getTransactionFactory() == null) {
			return new ManagedTransactionFactory();
		}
		return environment.getTransactionFactory();
	}

	private void closeTransaction(Transaction tx) {
		if (tx != null) {
			try {
				tx.close();
			} catch (SQLException ignore) {
			}
		}
	}

}
