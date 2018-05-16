package org.apache.ibatis.executor.resultset;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

//ResultSet处理器接口
public interface ResultSetHandler {

	//处理结果集
	<E> List<E> handleResultSets(Statement stmt) throws SQLException;

	//处理OUT参数
	void handleOutputParameters(CallableStatement cs) throws SQLException;

}
