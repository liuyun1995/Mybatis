package org.apache.ibatis.session.defaults;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.result.DefaultMapResultHandler;
import org.apache.ibatis.executor.result.DefaultResultContext;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

//默认SqlSession实现类
public class DefaultSqlSession implements SqlSession {

	private Configuration configuration; //配置信息
	private Executor executor;           //执行器
	private boolean autoCommit;         //是否自动提交
	private boolean dirty;              //是否是脏数据

	//构造器
	public DefaultSqlSession(Configuration configuration, Executor executor, boolean autoCommit) {
		this.configuration = configuration;
		this.executor = executor;
		this.dirty = false;
		this.autoCommit = autoCommit;
	}

	//构造器
	public DefaultSqlSession(Configuration configuration, Executor executor) {
		this(configuration, executor, false);
	}

	//查询方法(单条记录)
	public <T> T selectOne(String statement) {
		return this.<T>selectOne(statement, null);
	}

	//查询方法(单条记录)
	public <T> T selectOne(String statement, Object parameter) {
		//调用selectList方法进行查询
		List<T> list = this.<T>selectList(statement, parameter);
		//如果是一条数据，则返回该数据
		if (list.size() == 1) {
			return list.get(0);
		//如果有多条数据，则抛出异常
		} else if (list.size() > 1) {
			throw new TooManyResultsException(
					"Expected one result (or null) to be returned by selectOne(), but found: " + list.size());
		//如果没有数据，则返回null
		} else {
			return null;
		}
	}

	//查询方法(返回Map)
	public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
		return this.selectMap(statement, null, mapKey, RowBounds.DEFAULT);
	}

	//查询方法(返回Map)
	public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
		return this.selectMap(statement, parameter, mapKey, RowBounds.DEFAULT);
	}

	//查询方法(返回Map)
	public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
		//调用selectList方法查询
		final List<?> list = selectList(statement, parameter, rowBounds);
		//获取映射结果处理器
		final DefaultMapResultHandler<K, V> mapResultHandler = new DefaultMapResultHandler<K, V>(mapKey,
				configuration.getObjectFactory(), configuration.getObjectWrapperFactory());
		//获取结果上下文
		final DefaultResultContext context = new DefaultResultContext();
		//遍历查询结果集合
		for (Object o : list) {
			//将查询结果设置到结果上下文中
			context.nextResultObject(o);
			//调用结果处理器处理结果上下文
			mapResultHandler.handleResult(context);
		}
		//返回结果处理器执行后的Map
		return mapResultHandler.getMappedResults();
	}

	//查询方法(返回List)
	public <E> List<E> selectList(String statement) {
		return this.selectList(statement, null);
	}

	//查询方法(返回List)
	public <E> List<E> selectList(String statement, Object parameter) {
		return this.selectList(statement, parameter, RowBounds.DEFAULT);
	}

	//查询方法(返回List)
	public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
		try {
			//获取对应的MappedStatement
			MappedStatement ms = configuration.getMappedStatement(statement);
			//使用执行器来查询结果
			return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
		} catch (Exception e) {
			throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
		} finally {
			ErrorContext.instance().reset();
		}
	}

	//查询方法(无返回值)
	public void select(String statement, Object parameter, ResultHandler handler) {
		select(statement, parameter, RowBounds.DEFAULT, handler);
	}

	//查询方法(无返回值)
	public void select(String statement, ResultHandler handler) {
		select(statement, null, RowBounds.DEFAULT, handler);
	}

	//查询方法(无返回值)
	public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
		try {
			MappedStatement ms = configuration.getMappedStatement(statement);
			executor.query(ms, wrapCollection(parameter), rowBounds, handler);
		} catch (Exception e) {
			throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
		} finally {
			ErrorContext.instance().reset();
		}
	}

	//插入方法
	public int insert(String statement) {
		return insert(statement, null);
	}

	//插入方法
	public int insert(String statement, Object parameter) {
		return update(statement, parameter);
	}

	//更新方法
	public int update(String statement) {
		return update(statement, null);
	}

	//更新方法
	public int update(String statement, Object parameter) {
		try {
			//更新之前将dirty设为true
			dirty = true;
			MappedStatement ms = configuration.getMappedStatement(statement);
			//调用执行器的update方法
			return executor.update(ms, wrapCollection(parameter));
		} catch (Exception e) {
			throw ExceptionFactory.wrapException("Error updating database.  Cause: " + e, e);
		} finally {
			ErrorContext.instance().reset();
		}
	}

	//删除方法
	public int delete(String statement) {
		return update(statement, null);
	}

	//删除方法
	public int delete(String statement, Object parameter) {
		return update(statement, parameter);
	}

	//提交事务
	public void commit() {
		commit(false);
	}

	//提交事务
	public void commit(boolean force) {
		try {
			//调用执行器的commit方法
			executor.commit(isCommitOrRollbackRequired(force));
			//在提交之后将dirty设为false
			dirty = false;
		} catch (Exception e) {
			throw ExceptionFactory.wrapException("Error committing transaction.  Cause: " + e, e);
		} finally {
			ErrorContext.instance().reset();
		}
	}

	//回滚事务
	public void rollback() {
		rollback(false);
	}

	//回滚事务
	public void rollback(boolean force) {
		try {
			//调用执行器的回滚方法
			executor.rollback(isCommitOrRollbackRequired(force));
			//回滚之后将dirty设为false
			dirty = false;
		} catch (Exception e) {
			throw ExceptionFactory.wrapException("Error rolling back transaction.  Cause: " + e, e);
		} finally {
			ErrorContext.instance().reset();
		}
	}

	//刷新语句
	public List<BatchResult> flushStatements() {
		try {
			//转而用执行器来flushStatements
			return executor.flushStatements();
		} catch (Exception e) {
			throw ExceptionFactory.wrapException("Error flushing statements.  Cause: " + e, e);
		} finally {
			ErrorContext.instance().reset();
		}
	}

	//关闭会话
	public void close() {
		try {
			//转而用执行器来close
			executor.close(isCommitOrRollbackRequired(false));
			//每次close之后，dirty标志设为false
			dirty = false;
		} finally {
			ErrorContext.instance().reset();
		}
	}

	//获取配置信息
	public Configuration getConfiguration() {
		return configuration;
	}

	//获取映射器
	public <T> T getMapper(Class<T> type) {
		//最后会去调用MapperRegistry.getMapper
		return configuration.<T>getMapper(type, this);
	}

	//获取数据库连接
	public Connection getConnection() {
		try {
			return executor.getTransaction().getConnection();
		} catch (SQLException e) {
			throw ExceptionFactory.wrapException("Error getting a new connection.  Cause: " + e, e);
		}
	}

	//清空缓存
	public void clearCache() {
		//转而用执行器来clearLocalCache
		executor.clearLocalCache();
	}

	//是否强制提交或回滚
	private boolean isCommitOrRollbackRequired(boolean force) {
		return (!autoCommit && dirty) || force;
	}

	//把参数包装成Collection
	private Object wrapCollection(final Object object) {
		if (object instanceof Collection) {
			StrictMap<Object> map = new StrictMap<Object>();
			map.put("collection", object);
			if (object instanceof List) {
				map.put("list", object);
			}
			return map;
		} else if (object != null && object.getClass().isArray()) {
			StrictMap<Object> map = new StrictMap<Object>();
			map.put("array", object);
			return map;
		}
		//参数若不是集合型，直接返回原来值
		return object;
	}

	public static class StrictMap<V> extends HashMap<String, V> {

		private static final long serialVersionUID = -5741767162221585340L;

		//若找不到对应的key，直接抛出异常，而不是返回null
		@Override
		public V get(Object key) {
			if (!super.containsKey(key)) {
				throw new BindingException(
						"Parameter '" + key + "' not found. Available parameters are " + this.keySet());
			}
			return super.get(key);
		}

	}

}
