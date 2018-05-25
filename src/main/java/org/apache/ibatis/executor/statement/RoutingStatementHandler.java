package org.apache.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

//路由选择语句处理器
public class RoutingStatementHandler implements StatementHandler {

	private final StatementHandler delegate;

	public RoutingStatementHandler(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
			ResultHandler resultHandler, BoundSql boundSql) {
		//根据语句类型, 委派到不同的语句处理器, 默认为PREPARED
		//有这三种类型(STATEMENT|PREPARED|CALLABLE)
		switch (ms.getStatementType()) {
		case STATEMENT:
			delegate = new SimpleStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
			break;
		case PREPARED:
			delegate = new PreparedStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
			break;
		case CALLABLE:
			delegate = new CallableStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
			break;
		default:
			throw new ExecutorException("Unknown statement type: " + ms.getStatementType());
		}

	}

	public Statement prepare(Connection connection) throws SQLException {
		return delegate.prepare(connection);
	}

	public void parameterize(Statement statement) throws SQLException {
		delegate.parameterize(statement);
	}

	public void batch(Statement statement) throws SQLException {
		delegate.batch(statement);
	}

	public int update(Statement statement) throws SQLException {
		return delegate.update(statement);
	}

	public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
		return delegate.<E>query(statement, resultHandler);
	}

	public BoundSql getBoundSql() {
		return delegate.getBoundSql();
	}

	public ParameterHandler getParameterHandler() {
		return delegate.getParameterHandler();
	}
}
