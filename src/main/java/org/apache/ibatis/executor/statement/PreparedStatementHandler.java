package org.apache.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

//预处理语句处理器
public class PreparedStatementHandler extends BaseStatementHandler {

	public PreparedStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter,
			RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
		super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
	}

	public int update(Statement statement) throws SQLException {
		//调用PreparedStatement.execute和PreparedStatement.getUpdateCount
		PreparedStatement ps = (PreparedStatement) statement;
		ps.execute();
		int rows = ps.getUpdateCount();
		Object parameterObject = boundSql.getParameterObject();
		KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
		keyGenerator.processAfter(executor, mappedStatement, ps, parameterObject);
		return rows;
	}

	public void batch(Statement statement) throws SQLException {
		PreparedStatement ps = (PreparedStatement) statement;
		ps.addBatch();
	}

	//查询方法
	public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
		//强转为PreparedStatement对象
		PreparedStatement ps = (PreparedStatement) statement;
		//执行语句
		ps.execute();
		//使用结果集处理器来处理结果
		return resultSetHandler.<E>handleResultSets(ps);
	}

	@Override
	protected Statement instantiateStatement(Connection connection) throws SQLException {
		//获取原生sql语句
		String sql = boundSql.getSql();
		//如果是Jdbc3KeyGenerator主键生成器
		if (mappedStatement.getKeyGenerator() instanceof Jdbc3KeyGenerator) {
			String[] keyColumnNames = mappedStatement.getKeyColumns();
			if (keyColumnNames == null) {
				return connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
			} else {
				return connection.prepareStatement(sql, keyColumnNames);
			}
		//如果结果集类型不为空
		} else if (mappedStatement.getResultSetType() != null) {
			return connection.prepareStatement(sql, mappedStatement.getResultSetType().getValue(),
					ResultSet.CONCUR_READ_ONLY);
		} else {
			return connection.prepareStatement(sql);
		}
	}

	public void parameterize(Statement statement) throws SQLException {
		//调用参数处理器设置参数
		parameterHandler.setParameters((PreparedStatement) statement);
	}

}
