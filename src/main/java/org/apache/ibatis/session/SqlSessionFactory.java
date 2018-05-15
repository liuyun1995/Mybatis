package org.apache.ibatis.session;

import java.sql.Connection;

//SqlSession构建工厂
public interface SqlSessionFactory {
	
	SqlSession openSession();
	
	SqlSession openSession(boolean autoCommit);
	
	SqlSession openSession(Connection connection);
	
	SqlSession openSession(TransactionIsolationLevel level);
	
	SqlSession openSession(ExecutorType execType);

	SqlSession openSession(ExecutorType execType, boolean autoCommit);

	SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level);

	SqlSession openSession(ExecutorType execType, Connection connection);

	Configuration getConfiguration();

}
