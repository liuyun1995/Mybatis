package org.apache.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;

//基础语句处理器
public abstract class BaseStatementHandler implements StatementHandler {

	protected final Configuration configuration;
	protected final ObjectFactory objectFactory;
	protected final TypeHandlerRegistry typeHandlerRegistry;
	protected final ResultSetHandler resultSetHandler;
	protected final ParameterHandler parameterHandler;

	protected final Executor executor;
	protected final MappedStatement mappedStatement;
	protected final RowBounds rowBounds;

	protected BoundSql boundSql;

	protected BaseStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject,
			RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
		this.configuration = mappedStatement.getConfiguration();
		this.executor = executor;
		this.mappedStatement = mappedStatement;
		this.rowBounds = rowBounds;

		this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
		this.objectFactory = configuration.getObjectFactory();

		if (boundSql == null) { // issue #435, get the key before calculating the statement
			generateKeys(parameterObject);
			boundSql = mappedStatement.getBoundSql(parameterObject);
		}

		this.boundSql = boundSql;

		//生成parameterHandler
		this.parameterHandler = configuration.newParameterHandler(mappedStatement, parameterObject, boundSql);
		//生成resultSetHandler
		this.resultSetHandler = configuration.newResultSetHandler(executor, mappedStatement, rowBounds,
				parameterHandler, resultHandler, boundSql);
	}

	//获取绑定sql
	public BoundSql getBoundSql() {
		return boundSql;
	}

	//获取参数处理器
	public ParameterHandler getParameterHandler() {
		return parameterHandler;
	}

	//准备语句
	public Statement prepare(Connection connection) throws SQLException {
		ErrorContext.instance().sql(boundSql.getSql());
		Statement statement = null;
		try {
			//实例化Statement
			statement = instantiateStatement(connection);
			//设置超时
			setStatementTimeout(statement);
			//设置读取条数
			setFetchSize(statement);
			return statement;
		} catch (SQLException e) {
			closeStatement(statement);
			throw e;
		} catch (Exception e) {
			closeStatement(statement);
			throw new ExecutorException("Error preparing statement.  Cause: " + e, e);
		}
	}

	//实例化Statement方法
	protected abstract Statement instantiateStatement(Connection connection) throws SQLException;

	//设置超时
	protected void setStatementTimeout(Statement stmt) throws SQLException {
		Integer timeout = mappedStatement.getTimeout();
		Integer defaultTimeout = configuration.getDefaultStatementTimeout();
		if (timeout != null) {
			stmt.setQueryTimeout(timeout);
		} else if (defaultTimeout != null) {
			stmt.setQueryTimeout(defaultTimeout);
		}
	}

	//设置读取条数
	protected void setFetchSize(Statement stmt) throws SQLException {
		Integer fetchSize = mappedStatement.getFetchSize();
		if (fetchSize != null) {
			stmt.setFetchSize(fetchSize);
		}
	}

	//关闭语句
	protected void closeStatement(Statement statement) {
		try {
			if (statement != null) {
				statement.close();
			}
		} catch (SQLException e) {
			// ignore
		}
	}

	//生成key
	protected void generateKeys(Object parameter) {
		//获取键值生成器
		KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
		ErrorContext.instance().store();
		//在执行前生成
		keyGenerator.processBefore(executor, mappedStatement, null, parameter);
		ErrorContext.instance().recall();
	}

}
