package org.apache.ibatis.transaction;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;

//事务工厂
public interface TransactionFactory {

	//设置属性
	void setProperties(Properties props);

	//根据数据库连接创建事务
	Transaction newTransaction(Connection conn);

	//根据数据源和事务隔离级别创建事务
	Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit);

}
