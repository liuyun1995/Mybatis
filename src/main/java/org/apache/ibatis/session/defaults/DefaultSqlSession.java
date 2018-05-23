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

	public DefaultSqlSession(Configuration configuration, Executor executor, boolean autoCommit) {
		this.configuration = configuration;
		this.executor = executor;
		this.dirty = false;
		this.autoCommit = autoCommit;
	}

	public DefaultSqlSession(Configuration configuration, Executor executor) {
		this(configuration, executor, false);
	}

	public <T> T selectOne(String statement) {
		return this.<T>selectOne(statement, null);
	}

	//核心selectOne
	public <T> T selectOne(String statement, Object parameter) {
		// 转而去调用selectList,很简单的，如果得到0条则返回null，得到1条则返回1条，得到多条报TooManyResultsException错误
		// 特别需要注意的是当没有查询到结果的时候就会返回null。因此一般建议在mapper中编写resultType的时候使用包装类型
		// 而不是基本类型，比如推荐使用Integer而不是int。这样就可以避免NPE
		List<T> list = this.<T>selectList(statement, parameter);
		if (list.size() == 1) {
			return list.get(0);
		} else if (list.size() > 1) {
			throw new TooManyResultsException(
					"Expected one result (or null) to be returned by selectOne(), but found: " + list.size());
		} else {
			return null;
		}
	}

	public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
		return this.selectMap(statement, null, mapKey, RowBounds.DEFAULT);
	}

	public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
		return this.selectMap(statement, parameter, mapKey, RowBounds.DEFAULT);
	}

	//核心selectMap
	public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
		// 转而去调用selectList
		final List<?> list = selectList(statement, parameter, rowBounds);
		final DefaultMapResultHandler<K, V> mapResultHandler = new DefaultMapResultHandler<K, V>(mapKey,
				configuration.getObjectFactory(), configuration.getObjectWrapperFactory());
		final DefaultResultContext context = new DefaultResultContext();
		for (Object o : list) {
			// 循环用DefaultMapResultHandler处理每条记录
			context.nextResultObject(o);
			mapResultHandler.handleResult(context);
		}
		// 注意这个DefaultMapResultHandler里面存了所有已处理的记录(内部实现可能就是一个Map)，最后再返回一个Map
		return mapResultHandler.getMappedResults();
	}

	public <E> List<E> selectList(String statement) {
		return this.selectList(statement, null);
	}

	public <E> List<E> selectList(String statement, Object parameter) {
		return this.selectList(statement, parameter, RowBounds.DEFAULT);
	}

	//核心selectList
	public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
		try {
			//根据statement找到对应的MappedStatement
			MappedStatement ms = configuration.getMappedStatement(statement);
			//转而用执行器来查询结果,注意这里传入的ResultHandler是null
			return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
		} catch (Exception e) {
			throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
		} finally {
			ErrorContext.instance().reset();
		}
	}

	public void select(String statement, Object parameter, ResultHandler handler) {
		select(statement, parameter, RowBounds.DEFAULT, handler);
	}

	public void select(String statement, ResultHandler handler) {
		select(statement, null, RowBounds.DEFAULT, handler);
	}

	// 核心select,带有ResultHandler，和selectList代码差不多的，区别就一个ResultHandler
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

	//核心update
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

	//核心commit
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

	//回滚方法
	public void rollback() {
		rollback(false);
	}

	//核心rollback
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

	//核心flushStatements
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

	//核心close
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

	public Configuration getConfiguration() {
		return configuration;
	}

	public <T> T getMapper(Class<T> type) {
		//最后会去调用MapperRegistry.getMapper
		return configuration.<T>getMapper(type, this);
	}

	public Connection getConnection() {
		try {
			return executor.getTransaction().getConnection();
		} catch (SQLException e) {
			throw ExceptionFactory.wrapException("Error getting a new connection.  Cause: " + e, e);
		}
	}

	//核心clearCache
	public void clearCache() {
		// 转而用执行器来clearLocalCache
		executor.clearLocalCache();
	}

	//检查是否需要强制commit或rollback
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

	//严格的Map，如果找不到对应的key，直接抛BindingException例外，而不是返回null
	public static class StrictMap<V> extends HashMap<String, V> {

		private static final long serialVersionUID = -5741767162221585340L;

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
