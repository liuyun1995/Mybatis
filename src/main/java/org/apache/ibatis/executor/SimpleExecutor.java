package org.apache.ibatis.executor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

//简单执行器
public class SimpleExecutor extends BaseExecutor {

	public SimpleExecutor(Configuration configuration, Transaction transaction) {
		super(configuration, transaction);
	}
	
	@Override
	public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
		Statement stmt = null;
		try {
			//获取配置信息
			Configuration configuration = ms.getConfiguration();
			//新建语句处理器
			StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
			//获取准备语句
			stmt = prepareStatement(handler, ms.getStatementLog());
			//执行语句处理器的更新方法
			return handler.update(stmt);
		} finally {
			closeStatement(stmt);
		}
	}
	
	//执行查询方法
	@Override
	public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler,
			BoundSql boundSql) throws SQLException {
		Statement stmt = null;
		try {
			//获取配置信息
			Configuration configuration = ms.getConfiguration();
			//新建语句处理器
			StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
			//获取Statement对象, 在该方法中设置参数
			stmt = prepareStatement(handler, ms.getStatementLog());
			//调用语句处理器的查询方法
			return handler.<E>query(stmt, resultHandler);
		} finally {
			closeStatement(stmt);
		}
	}

	//冲刷语句
	@Override
	public List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
		//doFlushStatements只是给batch用的，所以这里返回空
		return Collections.emptyList();
	}

	//预处理语句
	private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
		Statement stmt;
		//获取数据库连接
		Connection connection = getConnection(statementLog);
		//调用语句处理器获取Statement对象
		stmt = handler.prepare(connection);
		//调用语句处理器参数化
		handler.parameterize(stmt);
		return stmt;
	}

}
