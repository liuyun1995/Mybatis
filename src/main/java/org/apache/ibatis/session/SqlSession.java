package org.apache.ibatis.session;

import java.io.Closeable;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.executor.BatchResult;

//对外提供操作的API
public interface SqlSession extends Closeable {
	
	<T> T selectOne(String statement);

	<T> T selectOne(String statement, Object parameter);

	<E> List<E> selectList(String statement);
	
	<E> List<E> selectList(String statement, Object parameter);
	
	<E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds);
	
	<K, V> Map<K, V> selectMap(String statement, String mapKey);
	
	<K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey);
	
	<K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds);
	
	void select(String statement, Object parameter, ResultHandler handler);
	
	void select(String statement, ResultHandler handler);
	
	void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler);
	
	int insert(String statement);
	
	int insert(String statement, Object parameter);
	
	int update(String statement);
	
	int update(String statement, Object parameter);
	
	int delete(String statement);
	
	int delete(String statement, Object parameter);
	
	void commit();
	
	void commit(boolean force);
	
	void rollback();
	
	void rollback(boolean force);
	
	//刷新语句
	List<BatchResult> flushStatements();

	//关闭会话
	void close();

	//清空缓存
	void clearCache();

	//获取配置
	Configuration getConfiguration();

	//获取映射器
	<T> T getMapper(Class<T> type);

	//获取连接
	Connection getConnection();
}
