package org.apache.ibatis.session;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.reflection.ExceptionUtil;

//SqlSession管理器, 实现了SqlSession接口
public class SqlSessionManager implements SqlSessionFactory, SqlSession {

	private final SqlSessionFactory sqlSessionFactory;   //sqlSession工厂
	private final SqlSession sqlSessionProxy;            //sqlSession代理

	private ThreadLocal<SqlSession> localSqlSession = new ThreadLocal<SqlSession>();

	//构造器
	private SqlSessionManager(SqlSessionFactory sqlSessionFactory) {
		this.sqlSessionFactory = sqlSessionFactory;
		//生成sqlSession代理, 这里使用的是JDK动态代理
		this.sqlSessionProxy = (SqlSession) Proxy.newProxyInstance(SqlSessionFactory.class.getClassLoader(),
				new Class[] { SqlSession.class }, new SqlSessionInterceptor());
	}

	//------------------------------------------实例化SqlSessionManager方法-------------------------------------------
	
	public static SqlSessionManager newInstance(Reader reader) {
		return new SqlSessionManager(new SqlSessionFactoryBuilder().build(reader, null, null));
	}

	public static SqlSessionManager newInstance(Reader reader, String environment) {
		return new SqlSessionManager(new SqlSessionFactoryBuilder().build(reader, environment, null));
	}

	public static SqlSessionManager newInstance(Reader reader, Properties properties) {
		return new SqlSessionManager(new SqlSessionFactoryBuilder().build(reader, null, properties));
	}

	public static SqlSessionManager newInstance(InputStream inputStream) {
		return new SqlSessionManager(new SqlSessionFactoryBuilder().build(inputStream, null, null));
	}

	public static SqlSessionManager newInstance(InputStream inputStream, String environment) {
		return new SqlSessionManager(new SqlSessionFactoryBuilder().build(inputStream, environment, null));
	}

	public static SqlSessionManager newInstance(InputStream inputStream, Properties properties) {
		return new SqlSessionManager(new SqlSessionFactoryBuilder().build(inputStream, null, properties));
	}

	public static SqlSessionManager newInstance(SqlSessionFactory sqlSessionFactory) {
		return new SqlSessionManager(sqlSessionFactory);
	}

	//------------------------------------------实例化SqlSessionManager方法-------------------------------------------
	
	public void startManagedSession() {
		this.localSqlSession.set(openSession());
	}

	public void startManagedSession(boolean autoCommit) {
		this.localSqlSession.set(openSession(autoCommit));
	}

	public void startManagedSession(Connection connection) {
		this.localSqlSession.set(openSession(connection));
	}

	public void startManagedSession(TransactionIsolationLevel level) {
		this.localSqlSession.set(openSession(level));
	}

	public void startManagedSession(ExecutorType execType) {
		this.localSqlSession.set(openSession(execType));
	}

	public void startManagedSession(ExecutorType execType, boolean autoCommit) {
		this.localSqlSession.set(openSession(execType, autoCommit));
	}

	public void startManagedSession(ExecutorType execType, TransactionIsolationLevel level) {
		this.localSqlSession.set(openSession(execType, level));
	}

	public void startManagedSession(ExecutorType execType, Connection connection) {
		this.localSqlSession.set(openSession(execType, connection));
	}

	public boolean isManagedSessionStarted() {
		return this.localSqlSession.get() != null;
	}

	public SqlSession openSession() {
		return sqlSessionFactory.openSession();
	}

	public SqlSession openSession(boolean autoCommit) {
		return sqlSessionFactory.openSession(autoCommit);
	}

	public SqlSession openSession(Connection connection) {
		return sqlSessionFactory.openSession(connection);
	}

	public SqlSession openSession(TransactionIsolationLevel level) {
		return sqlSessionFactory.openSession(level);
	}

	public SqlSession openSession(ExecutorType execType) {
		return sqlSessionFactory.openSession(execType);
	}

	public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
		return sqlSessionFactory.openSession(execType, autoCommit);
	}

	public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
		return sqlSessionFactory.openSession(execType, level);
	}

	public SqlSession openSession(ExecutorType execType, Connection connection) {
		return sqlSessionFactory.openSession(execType, connection);
	}

	public Configuration getConfiguration() {
		return sqlSessionFactory.getConfiguration();
	}

	public <T> T selectOne(String statement) {
		return sqlSessionProxy.<T>selectOne(statement);
	}

	public <T> T selectOne(String statement, Object parameter) {
		return sqlSessionProxy.<T>selectOne(statement, parameter);
	}

	public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
		return sqlSessionProxy.<K, V>selectMap(statement, mapKey);
	}

	public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
		return sqlSessionProxy.<K, V>selectMap(statement, parameter, mapKey);
	}

	public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
		return sqlSessionProxy.<K, V>selectMap(statement, parameter, mapKey, rowBounds);
	}

	public <E> List<E> selectList(String statement) {
		return sqlSessionProxy.<E>selectList(statement);
	}

	public <E> List<E> selectList(String statement, Object parameter) {
		return sqlSessionProxy.<E>selectList(statement, parameter);
	}

	public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
		return sqlSessionProxy.<E>selectList(statement, parameter, rowBounds);
	}

	public void select(String statement, ResultHandler handler) {
		sqlSessionProxy.select(statement, handler);
	}

	public void select(String statement, Object parameter, ResultHandler handler) {
		sqlSessionProxy.select(statement, parameter, handler);
	}

	public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
		sqlSessionProxy.select(statement, parameter, rowBounds, handler);
	}

	public int insert(String statement) {
		return sqlSessionProxy.insert(statement);
	}

	public int insert(String statement, Object parameter) {
		return sqlSessionProxy.insert(statement, parameter);
	}

	public int update(String statement) {
		return sqlSessionProxy.update(statement);
	}

	public int update(String statement, Object parameter) {
		return sqlSessionProxy.update(statement, parameter);
	}

	public int delete(String statement) {
		return sqlSessionProxy.delete(statement);
	}

	public int delete(String statement, Object parameter) {
		return sqlSessionProxy.delete(statement, parameter);
	}

	//获取Mapper
	public <T> T getMapper(Class<T> type) {
		return getConfiguration().getMapper(type, this);
	}

	//获取数据库连接
	public Connection getConnection() {
		final SqlSession sqlSession = localSqlSession.get();
		if (sqlSession == null) {
			throw new SqlSessionException("Error:  Cannot get connection.  No managed session is started.");
		}
		return sqlSession.getConnection();
	}

	//清空缓存
	public void clearCache() {
		final SqlSession sqlSession = localSqlSession.get();
		if (sqlSession == null) {
			throw new SqlSessionException("Error:  Cannot clear the cache.  No managed session is started.");
		}
		sqlSession.clearCache();
	}

	//提交事务
	public void commit() {
		final SqlSession sqlSession = localSqlSession.get();
		if (sqlSession == null) {
			throw new SqlSessionException("Error:  Cannot commit.  No managed session is started.");
		}
		sqlSession.commit();
	}

	//提交事务
	public void commit(boolean force) {
		final SqlSession sqlSession = localSqlSession.get();
		if (sqlSession == null) {
			throw new SqlSessionException("Error:  Cannot commit.  No managed session is started.");
		}
		sqlSession.commit(force);
	}

	//回滚事务
	public void rollback() {
		final SqlSession sqlSession = localSqlSession.get();
		if (sqlSession == null) {
			throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
		}
		sqlSession.rollback();
	}

	//回滚事务
	public void rollback(boolean force) {
		final SqlSession sqlSession = localSqlSession.get();
		if (sqlSession == null) {
			throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
		}
		sqlSession.rollback(force);
	}

	//刷新语句
	public List<BatchResult> flushStatements() {
		//获取当前线程的会话
		final SqlSession sqlSession = localSqlSession.get();
		if (sqlSession == null) {
			throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
		}
		return sqlSession.flushStatements();
	}

	//关闭会话
	public void close() {
		final SqlSession sqlSession = localSqlSession.get();
		if (sqlSession == null) {
			throw new SqlSessionException("Error:  Cannot close.  No managed session is started.");
		}
		try {
			sqlSession.close();
		} finally {
			localSqlSession.set(null);
		}
	}

	//sqlSession代理
	private class SqlSessionInterceptor implements InvocationHandler {
		
		//空的构造器, 不需传入会话, 在调用时会根据情况创建会话
		public SqlSessionInterceptor() {}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			//获取当前线程的会话
			final SqlSession sqlSession = SqlSessionManager.this.localSqlSession.get();
			//若当前线程存在SqlSession则直接调用
			if (sqlSession != null) {
				try {
					return method.invoke(sqlSession, args);
				} catch (Throwable t) {
					throw ExceptionUtil.unwrapThrowable(t);
				}
			//否则调用openSession方法获取sqlSession再调用
			} else {
				//获取新的会话对象
				final SqlSession autoSqlSession = openSession();
				try {
					//执行方法
					final Object result = method.invoke(autoSqlSession, args);
					//提交事务
					autoSqlSession.commit();
					//返回结果
					return result;
				} catch (Throwable t) {
					//出现异常回滚事务
					autoSqlSession.rollback();
					throw ExceptionUtil.unwrapThrowable(t);
				} finally {
					//最后关闭会话
					autoSqlSession.close();
				}
			}
		}
	}

}
