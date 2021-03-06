package org.apache.ibatis.executor;

import static org.apache.ibatis.executor.ExecutionPlaceholder.EXECUTION_PLACEHOLDER;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.logging.jdbc.ConnectionLogger;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.type.TypeHandlerRegistry;

//基础执行器
public abstract class BaseExecutor implements Executor {

	private static final Log log = LogFactory.getLog(BaseExecutor.class);

	protected Transaction transaction;                             //事务
	protected Executor wrapper;                                    //执行器
	protected ConcurrentLinkedQueue<DeferredLoad> deferredLoads;   //延迟加载队列
	protected PerpetualCache localCache;                           //本地缓存
	protected PerpetualCache localOutputParameterCache;            //本地输出参数缓存
	protected Configuration configuration;                         //配置信息
	protected int queryStack = 0;                                  //查询堆栈
	private boolean closed;

	protected BaseExecutor(Configuration configuration, Transaction transaction) {
		this.transaction = transaction;
		this.deferredLoads = new ConcurrentLinkedQueue<DeferredLoad>();
		//设置本地缓存, 默认为永久缓存
		this.localCache = new PerpetualCache("LocalCache");
		this.localOutputParameterCache = new PerpetualCache("LocalOutputParameterCache");
		this.closed = false;
		this.configuration = configuration;
		this.wrapper = this;
	}

	//获取事务
	public Transaction getTransaction() {
		if (closed) {
			throw new ExecutorException("Executor was closed.");
		}
		return transaction;
	}

	//关闭执行器
	public void close(boolean forceRollback) {
		try {
			try {
				//回滚事务
				rollback(forceRollback);
			} finally {
				if (transaction != null) {
					transaction.close();
				}
			}
		} catch (SQLException e) {
			log.warn("Unexpected exception on closing transaction.  Cause: " + e);
		} finally {
			transaction = null;
			deferredLoads = null;
			localCache = null;
			localOutputParameterCache = null;
			closed = true;
		}
	}

	//是否已关闭
	public boolean isClosed() {
		return closed;
	}

	//更新方法
	public int update(MappedStatement ms, Object parameter) throws SQLException {
		ErrorContext.instance().resource(ms.getResource()).activity("executing an update").object(ms.getId());
		if (closed) {
			throw new ExecutorException("Executor was closed.");
		}
		//先清局部缓存，再更新，如何更新交由子类，模板方法模式
		clearLocalCache();
		return doUpdate(ms, parameter);
	}

	//刷新语句
	public List<BatchResult> flushStatements() throws SQLException {
		return flushStatements(false);
	}

	//刷新语句
	public List<BatchResult> flushStatements(boolean isRollBack) throws SQLException {
		if (closed) {
			throw new ExecutorException("Executor was closed.");
		}
		return doFlushStatements(isRollBack);
	}

	//查询方法
	public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler)
			throws SQLException {
		//获取绑定sql
		BoundSql boundSql = ms.getBoundSql(parameter);
		//创建缓存Key
		CacheKey key = createCacheKey(ms, parameter, rowBounds, boundSql);
		//执行查询
		return query(ms, parameter, rowBounds, resultHandler, key, boundSql);
	}

	//查询方法
	@SuppressWarnings("unchecked")
	public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler,
			CacheKey key, BoundSql boundSql) throws SQLException {
		ErrorContext.instance().resource(ms.getResource()).activity("executing a query").object(ms.getId());
		//若执行器已关闭则报错
		if (closed) {
			throw new ExecutorException("Executor was closed.");
		}
		//先清局部缓存, 再查询.但仅查询堆栈为0，才清。为了处理递归调用
		if (queryStack == 0 && ms.isFlushCacheRequired()) {
			clearLocalCache();
		}
		List<E> list;
		try {
			//加一,这样递归调用到上面的时候就不会再清局部缓存了
			queryStack++;
			//先从本地缓存查询
			list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
			if (list != null) {
				//若从本地缓存查到
				handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
			} else {
				//若本地缓存不存在, 则从数据库查
				list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
			}
		} finally {
			//清空堆栈
			queryStack--;
		}
		if (queryStack == 0) {
			//延迟加载队列中所有元素
			for (DeferredLoad deferredLoad : deferredLoads) {
				deferredLoad.load();
			}
			//清空延迟加载队列
			deferredLoads.clear();
			if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
				//如果是STATEMENT，清本地缓存
				clearLocalCache();
			}
		}
		return list;
	}

	//延迟加载，DefaultResultSetHandler.getNestedQueryMappingValue调用.属于嵌套查询，比较高级.
	public void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key,
			Class<?> targetType) {
		if (closed) {
			throw new ExecutorException("Executor was closed.");
		}
		DeferredLoad deferredLoad = new DeferredLoad(resultObject, property, key, localCache, configuration,
				targetType);
		// 如果能加载，则立刻加载，否则加入到延迟加载队列中
		if (deferredLoad.canLoad()) {
			deferredLoad.load();
		} else {
			// 这里怎么又new了一个新的，性能有点问题
			deferredLoads.add(new DeferredLoad(resultObject, property, key, localCache, configuration, targetType));
		}
	}

	//创建缓存Key
	public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
		if (closed) {
			throw new ExecutorException("Executor was closed.");
		}
		CacheKey cacheKey = new CacheKey();
		// MyBatis对于其 Key 的生成采取规则为：[mappedStementId + offset + limit + SQL +
		// queryParams + environment]生成一个哈希码
		cacheKey.update(ms.getId());
		cacheKey.update(Integer.valueOf(rowBounds.getOffset()));
		cacheKey.update(Integer.valueOf(rowBounds.getLimit()));
		cacheKey.update(boundSql.getSql());
		List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
		TypeHandlerRegistry typeHandlerRegistry = ms.getConfiguration().getTypeHandlerRegistry();
		// mimic DefaultParameterHandler logic
		// 模仿DefaultParameterHandler的逻辑,不再重复，请参考DefaultParameterHandler
		for (int i = 0; i < parameterMappings.size(); i++) {
			ParameterMapping parameterMapping = parameterMappings.get(i);
			if (parameterMapping.getMode() != ParameterMode.OUT) {
				Object value;
				String propertyName = parameterMapping.getProperty();
				if (boundSql.hasAdditionalParameter(propertyName)) {
					value = boundSql.getAdditionalParameter(propertyName);
				} else if (parameterObject == null) {
					value = null;
				} else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
					value = parameterObject;
				} else {
					MetaObject metaObject = configuration.newMetaObject(parameterObject);
					value = metaObject.getValue(propertyName);
				}
				cacheKey.update(value);
			}
		}
		if (configuration.getEnvironment() != null) {
			cacheKey.update(configuration.getEnvironment().getId());
		}
		return cacheKey;
	}

	public boolean isCached(MappedStatement ms, CacheKey key) {
		return localCache.getObject(key) != null;
	}

	public void commit(boolean required) throws SQLException {
		if (closed) {
			throw new ExecutorException("Cannot commit, transaction is already closed");
		}
		clearLocalCache();
		flushStatements();
		if (required) {
			transaction.commit();
		}
	}

	//回滚事务
	public void rollback(boolean required) throws SQLException {
		if (!closed) {
			try {
				//清空本地缓存
				clearLocalCache();
				//刷新Statement
				flushStatements(true);
			} finally {
				if (required) {
					transaction.rollback();
				}
			}
		}
	}

	//清空本地缓存
	public void clearLocalCache() {
		if (!closed) {
			localCache.clear();
			localOutputParameterCache.clear();
		}
	}

	protected abstract int doUpdate(MappedStatement ms, Object parameter) throws SQLException;

	protected abstract List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException;

	//query-->queryFromDatabase-->doQuery
	protected abstract <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds,
			ResultHandler resultHandler, BoundSql boundSql) throws SQLException;

	//关闭语句
	protected void closeStatement(Statement statement) {
		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
			}
		}
	}

	//处理本地缓存输出参数
	private void handleLocallyCachedOutputParameters(MappedStatement ms, CacheKey key, Object parameter,
			BoundSql boundSql) {
		//处理存储过程的OUT参数
		if (ms.getStatementType() == StatementType.CALLABLE) {
			final Object cachedParameter = localOutputParameterCache.getObject(key);
			if (cachedParameter != null && parameter != null) {
				final MetaObject metaCachedParameter = configuration.newMetaObject(cachedParameter);
				final MetaObject metaParameter = configuration.newMetaObject(parameter);
				for (ParameterMapping parameterMapping : boundSql.getParameterMappings()) {
					if (parameterMapping.getMode() != ParameterMode.IN) {
						//获取参数名
						final String parameterName = parameterMapping.getProperty();
						//根据参数名获取缓存值
						final Object cachedValue = metaCachedParameter.getValue(parameterName);
						metaParameter.setValue(parameterName, cachedValue);
					}
				}
			}
		}
	}

	//从数据库中查询
	private <E> List<E> queryFromDatabase(MappedStatement ms, Object parameter, RowBounds rowBounds,
			ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
		List<E> list;
		//先向缓存中放入占位符
		localCache.putObject(key, EXECUTION_PLACEHOLDER);
		try {
			list = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
		} finally {
			//最后删除在本地缓存的占位符
			localCache.removeObject(key);
		}
		//查询出来后再添加到缓存
		localCache.putObject(key, list);
		//如果是存储过程, OUT参数也加入缓存
		if (ms.getStatementType() == StatementType.CALLABLE) {
			localOutputParameterCache.putObject(key, parameter);
		}
		return list;
	}

	//关闭连接
	protected Connection getConnection(Log statementLog) throws SQLException {
		Connection connection = transaction.getConnection();
		if (statementLog.isDebugEnabled()) {
			//如果需要打印Connection的日志，返回一个ConnectionLogger(代理模式, AOP思想)
			return ConnectionLogger.newInstance(connection, statementLog, queryStack);
		} else {
			return connection;
		}
	}

	public void setExecutorWrapper(Executor wrapper) {
		this.wrapper = wrapper;
	}

	//延迟加载
	private static class DeferredLoad {

		private final MetaObject resultObject;
		private final String property;
		private final Class<?> targetType;
		private final CacheKey key;
		private final PerpetualCache localCache;
		private final ObjectFactory objectFactory;
		private final ResultExtractor resultExtractor;
		
		public DeferredLoad(MetaObject resultObject, String property, CacheKey key, PerpetualCache localCache,
				Configuration configuration, Class<?> targetType) {
			this.resultObject = resultObject;
			this.property = property;
			this.key = key;
			this.localCache = localCache;
			this.objectFactory = configuration.getObjectFactory();
			this.resultExtractor = new ResultExtractor(configuration, objectFactory);
			this.targetType = targetType;
		}

		//是否可以加载
		public boolean canLoad() {
			//缓存中找到，且不为占位符，代表可以加载
			return localCache.getObject(key) != null && localCache.getObject(key) != EXECUTION_PLACEHOLDER;
		}

		// 加载
		public void load() {
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) localCache.getObject(key);
			Object value = resultExtractor.extractObjectFromList(list, targetType);
			resultObject.setValue(property, value);
		}

	}

}
