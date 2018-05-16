package org.apache.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.ResultHandler;

//语句处理器接口
public interface StatementHandler {

	//准备语句
	Statement prepare(Connection connection) throws SQLException;

	//参数化
	void parameterize(Statement statement) throws SQLException;

	//批处理
	void batch(Statement statement) throws SQLException;

	//update
	int update(Statement statement) throws SQLException;

	//select
	<E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException;

	//获取绑定SQL
	BoundSql getBoundSql();

	//获取参数处理器
	ParameterHandler getParameterHandler();

}
