package org.apache.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

//简单语句处理器
public class SimpleStatementHandler extends BaseStatementHandler {

	//构造器
	public SimpleStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter,
			RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
		super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
	}

	//更新方法
	public int update(Statement statement) throws SQLException {
		//获取原生sql语句
		String sql = boundSql.getSql();
		//获取参数对象
		Object parameterObject = boundSql.getParameterObject();
		//获取主键生成器
		KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
		int rows;
		//如果主键生成器是Jdbc3KeyGenerator
		if (keyGenerator instanceof Jdbc3KeyGenerator) {
			statement.execute(sql, Statement.RETURN_GENERATED_KEYS);
			rows = statement.getUpdateCount();
			keyGenerator.processAfter(executor, mappedStatement, statement, parameterObject);
		//如果主键生成器是SelectKeyGenerator
		} else if (keyGenerator instanceof SelectKeyGenerator) {
			statement.execute(sql);
			rows = statement.getUpdateCount();
			keyGenerator.processAfter(executor, mappedStatement, statement, parameterObject);
		//如果没有主键生成器
		} else {
			//使用Statement执行sql
			statement.execute(sql);
			//获取更新的行数
			rows = statement.getUpdateCount();
		}
		return rows;
	}

	public void batch(Statement statement) throws SQLException {
		//获取原生sql语句
		String sql = boundSql.getSql();
		//调用Statement.addBatch
		statement.addBatch(sql);
	}

	//查询方法
	public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
		//获取原生sql
		String sql = boundSql.getSql();
		//使用jdbc来执行sql
		statement.execute(sql);
		//使用结果处理器处理结果
		return resultSetHandler.<E>handleResultSets(statement);
	}

	//初始化语句
	@Override
	protected Statement instantiateStatement(Connection connection) throws SQLException {
		//调用Connection.createStatement
		if (mappedStatement.getResultSetType() != null) {
			return connection.createStatement(mappedStatement.getResultSetType().getValue(),
					ResultSet.CONCUR_READ_ONLY);
		} else {
			return connection.createStatement();
		}
	}

	public void parameterize(Statement statement) throws SQLException {}

}
