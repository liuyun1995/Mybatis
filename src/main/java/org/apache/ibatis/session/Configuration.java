package org.apache.ibatis.session;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ibatis.binding.MapperRegistry;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.builder.annotation.MethodResolver;
import org.apache.ibatis.builder.xml.XMLStatementBuilder;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.FifoCache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.SoftCache;
import org.apache.ibatis.cache.decorators.WeakCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.datasource.jndi.JndiDataSourceFactory;
import org.apache.ibatis.datasource.pooled.PooledDataSourceFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;
import org.apache.ibatis.executor.BatchExecutor;
import org.apache.ibatis.executor.CachingExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ReuseExecutor;
import org.apache.ibatis.executor.SimpleExecutor;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.executor.loader.cglib.CglibProxyFactory;
import org.apache.ibatis.executor.loader.javassist.JavassistProxyFactory;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl;
import org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl;
import org.apache.ibatis.logging.log4j.Log4jImpl;
import org.apache.ibatis.logging.log4j2.Log4j2Impl;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.InterceptorChain;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.LanguageDriverRegistry;
import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandlerRegistry;

//配置代表类
public class Configuration {

	//运行环境
	protected Environment environment;

	//----------以下都是<settings>节点----------
	//是否安全的行界
	protected boolean safeRowBoundsEnabled = false;
	//是否安全的结果处理
	protected boolean safeResultHandlerEnabled = true;
	//是否将下划线转成驼峰
	protected boolean mapUnderscoreToCamelCase = false;
	protected boolean aggressiveLazyLoading = true;
	//是否允许返回多结果集
	protected boolean multipleResultSetsEnabled = true;
	//是否自动生成主键
	protected boolean useGeneratedKeys = false;
	//是否使用列标签名
	protected boolean useColumnLabel = true;
	//是否开启缓存
	protected boolean cacheEnabled = true;
	protected boolean callSettersOnNulls = false;

	//日志前缀
	protected String logPrefix;
	//日志实现类
	protected Class<? extends Log> logImpl;
	//本地缓存范围
	protected LocalCacheScope localCacheScope = LocalCacheScope.SESSION;
	protected JdbcType jdbcTypeForNull = JdbcType.OTHER;
	protected Set<String> lazyLoadTriggerMethods = new HashSet<String>(Arrays.asList(new String[] { "equals", "clone", "hashCode", "toString" }));
	//默认超时时间
	protected Integer defaultStatementTimeout;
	//默认执行器类型
	protected ExecutorType defaultExecutorType = ExecutorType.SIMPLE;
	//自动映射行为
	protected AutoMappingBehavior autoMappingBehavior = AutoMappingBehavior.PARTIAL;
	//----------以上都是<settings>节点----------

	//变量属性
	protected Properties variables = new Properties();
	//对象工厂
	protected ObjectFactory objectFactory = new DefaultObjectFactory();
	//对象包装器工厂
	protected ObjectWrapperFactory objectWrapperFactory = new DefaultObjectWrapperFactory();
	//mapper注册器
	protected MapperRegistry mapperRegistry = new MapperRegistry(this);

	//是否开启懒加载
	protected boolean lazyLoadingEnabled = false;
	//代理工厂
	protected ProxyFactory proxyFactory = new JavassistProxyFactory();

	//数据库ID
	protected String databaseId;
	//配置工厂
	protected Class<?> configurationFactory;

	//拦截链
	protected final InterceptorChain interceptorChain = new InterceptorChain();
	//类型处理器注册器
	protected final TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();
	//类型别名注册器
	protected final TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();
	//语言驱动注册器
	protected final LanguageDriverRegistry languageRegistry = new LanguageDriverRegistry();

	//语句映射
	protected final Map<String, MappedStatement> mappedStatements = new StrictMap<MappedStatement>("Mapped Statements collection");
	//缓存映射
	protected final Map<String, Cache> caches = new StrictMap<Cache>("Caches collection");
	//结果映射
	protected final Map<String, ResultMap> resultMaps = new StrictMap<ResultMap>("Result Maps collection");
	//参数映射
	protected final Map<String, ParameterMap> parameterMaps = new StrictMap<ParameterMap>("Parameter Maps collection");
	//主键生成器映射
	protected final Map<String, KeyGenerator> keyGenerators = new StrictMap<KeyGenerator>("Key Generators collection");

	//已加载的xml资源路径
	protected final Set<String> loadedResources = new HashSet<String>();
	//SQL语句片映射
	protected final Map<String, XNode> sqlFragments = new StrictMap<XNode>("XML fragments parsed from previous mappers");

	//语句集合
	protected final Collection<XMLStatementBuilder> incompleteStatements = new LinkedList<XMLStatementBuilder>();
	//缓存关系集合
	protected final Collection<CacheRefResolver> incompleteCacheRefs = new LinkedList<CacheRefResolver>();
	//结果映射集合
	protected final Collection<ResultMapResolver> incompleteResultMaps = new LinkedList<ResultMapResolver>();
	//方法集合
	protected final Collection<MethodResolver> incompleteMethods = new LinkedList<MethodResolver>();

	//缓存关系映射
	protected final Map<String, String> cacheRefMap = new HashMap<String, String>();

	public Configuration(Environment environment) {
		this();
		this.environment = environment;
	}

	public Configuration() {
		//注册事务管理器类型别名
		typeAliasRegistry.registerAlias("JDBC", JdbcTransactionFactory.class);
		typeAliasRegistry.registerAlias("MANAGED", ManagedTransactionFactory.class);

		//注册数据源类型别名
		typeAliasRegistry.registerAlias("JNDI", JndiDataSourceFactory.class);
		typeAliasRegistry.registerAlias("POOLED", PooledDataSourceFactory.class);
		typeAliasRegistry.registerAlias("UNPOOLED", UnpooledDataSourceFactory.class);

		//注册缓存类型别名
		typeAliasRegistry.registerAlias("PERPETUAL", PerpetualCache.class);
		typeAliasRegistry.registerAlias("FIFO", FifoCache.class);
		typeAliasRegistry.registerAlias("LRU", LruCache.class);
		typeAliasRegistry.registerAlias("SOFT", SoftCache.class);
		typeAliasRegistry.registerAlias("WEAK", WeakCache.class);

		//注册数据库ID提供器类型别名
		typeAliasRegistry.registerAlias("DB_VENDOR", VendorDatabaseIdProvider.class);

		//注册语言驱动类型别名
		typeAliasRegistry.registerAlias("XML", XMLLanguageDriver.class);
		typeAliasRegistry.registerAlias("RAW", RawLanguageDriver.class);

		//注册日志类型别名
		typeAliasRegistry.registerAlias("SLF4J", Slf4jImpl.class);
		typeAliasRegistry.registerAlias("COMMONS_LOGGING", JakartaCommonsLoggingImpl.class);
		typeAliasRegistry.registerAlias("LOG4J", Log4jImpl.class);
		typeAliasRegistry.registerAlias("LOG4J2", Log4j2Impl.class);
		typeAliasRegistry.registerAlias("JDK_LOGGING", Jdk14LoggingImpl.class);
		typeAliasRegistry.registerAlias("STDOUT_LOGGING", StdOutImpl.class);
		typeAliasRegistry.registerAlias("NO_LOGGING", NoLoggingImpl.class);

		//注册代理工厂类型别名
		typeAliasRegistry.registerAlias("CGLIB", CglibProxyFactory.class);
		typeAliasRegistry.registerAlias("JAVASSIST", JavassistProxyFactory.class);

		languageRegistry.setDefaultDriverClass(XMLLanguageDriver.class);
		languageRegistry.register(RawLanguageDriver.class);
	}

	public String getLogPrefix() {
		return logPrefix;
	}

	public void setLogPrefix(String logPrefix) {
		this.logPrefix = logPrefix;
	}

	public Class<? extends Log> getLogImpl() {
		return logImpl;
	}

	@SuppressWarnings("unchecked")
	public void setLogImpl(Class<?> logImpl) {
		if (logImpl != null) {
			this.logImpl = (Class<? extends Log>) logImpl;
			LogFactory.useCustomLogging(this.logImpl);
		}
	}

	public boolean isCallSettersOnNulls() {
		return callSettersOnNulls;
	}

	public void setCallSettersOnNulls(boolean callSettersOnNulls) {
		this.callSettersOnNulls = callSettersOnNulls;
	}

	public String getDatabaseId() {
		return databaseId;
	}

	public void setDatabaseId(String databaseId) {
		this.databaseId = databaseId;
	}

	public Class<?> getConfigurationFactory() {
		return configurationFactory;
	}

	public void setConfigurationFactory(Class<?> configurationFactory) {
		this.configurationFactory = configurationFactory;
	}

	public boolean isSafeResultHandlerEnabled() {
		return safeResultHandlerEnabled;
	}

	public void setSafeResultHandlerEnabled(boolean safeResultHandlerEnabled) {
		this.safeResultHandlerEnabled = safeResultHandlerEnabled;
	}

	public boolean isSafeRowBoundsEnabled() {
		return safeRowBoundsEnabled;
	}

	public void setSafeRowBoundsEnabled(boolean safeRowBoundsEnabled) {
		this.safeRowBoundsEnabled = safeRowBoundsEnabled;
	}

	public boolean isMapUnderscoreToCamelCase() {
		return mapUnderscoreToCamelCase;
	}

	public void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase) {
		this.mapUnderscoreToCamelCase = mapUnderscoreToCamelCase;
	}

	public void addLoadedResource(String resource) {
		loadedResources.add(resource);
	}

	public boolean isResourceLoaded(String resource) {
		return loadedResources.contains(resource);
	}

	public Environment getEnvironment() {
		return environment;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public AutoMappingBehavior getAutoMappingBehavior() {
		return autoMappingBehavior;
	}

	public void setAutoMappingBehavior(AutoMappingBehavior autoMappingBehavior) {
		this.autoMappingBehavior = autoMappingBehavior;
	}

	public boolean isLazyLoadingEnabled() {
		return lazyLoadingEnabled;
	}

	public void setLazyLoadingEnabled(boolean lazyLoadingEnabled) {
		this.lazyLoadingEnabled = lazyLoadingEnabled;
	}

	public ProxyFactory getProxyFactory() {
		return proxyFactory;
	}

	public void setProxyFactory(ProxyFactory proxyFactory) {
		if (proxyFactory == null) {
			proxyFactory = new JavassistProxyFactory();
		}
		this.proxyFactory = proxyFactory;
	}

	public boolean isAggressiveLazyLoading() {
		return aggressiveLazyLoading;
	}

	public void setAggressiveLazyLoading(boolean aggressiveLazyLoading) {
		this.aggressiveLazyLoading = aggressiveLazyLoading;
	}

	public boolean isMultipleResultSetsEnabled() {
		return multipleResultSetsEnabled;
	}

	public void setMultipleResultSetsEnabled(boolean multipleResultSetsEnabled) {
		this.multipleResultSetsEnabled = multipleResultSetsEnabled;
	}

	public Set<String> getLazyLoadTriggerMethods() {
		return lazyLoadTriggerMethods;
	}

	public void setLazyLoadTriggerMethods(Set<String> lazyLoadTriggerMethods) {
		this.lazyLoadTriggerMethods = lazyLoadTriggerMethods;
	}

	public boolean isUseGeneratedKeys() {
		return useGeneratedKeys;
	}

	public void setUseGeneratedKeys(boolean useGeneratedKeys) {
		this.useGeneratedKeys = useGeneratedKeys;
	}

	public ExecutorType getDefaultExecutorType() {
		return defaultExecutorType;
	}

	public void setDefaultExecutorType(ExecutorType defaultExecutorType) {
		this.defaultExecutorType = defaultExecutorType;
	}

	public boolean isCacheEnabled() {
		return cacheEnabled;
	}

	public void setCacheEnabled(boolean cacheEnabled) {
		this.cacheEnabled = cacheEnabled;
	}

	public Integer getDefaultStatementTimeout() {
		return defaultStatementTimeout;
	}

	public void setDefaultStatementTimeout(Integer defaultStatementTimeout) {
		this.defaultStatementTimeout = defaultStatementTimeout;
	}

	public boolean isUseColumnLabel() {
		return useColumnLabel;
	}

	public void setUseColumnLabel(boolean useColumnLabel) {
		this.useColumnLabel = useColumnLabel;
	}

	public LocalCacheScope getLocalCacheScope() {
		return localCacheScope;
	}

	public void setLocalCacheScope(LocalCacheScope localCacheScope) {
		this.localCacheScope = localCacheScope;
	}

	public JdbcType getJdbcTypeForNull() {
		return jdbcTypeForNull;
	}

	public void setJdbcTypeForNull(JdbcType jdbcTypeForNull) {
		this.jdbcTypeForNull = jdbcTypeForNull;
	}

	public Properties getVariables() {
		return variables;
	}

	public void setVariables(Properties variables) {
		this.variables = variables;
	}

	public TypeHandlerRegistry getTypeHandlerRegistry() {
		return typeHandlerRegistry;
	}

	public TypeAliasRegistry getTypeAliasRegistry() {
		return typeAliasRegistry;
	}
	
	public MapperRegistry getMapperRegistry() {
		return mapperRegistry;
	}

	public ObjectFactory getObjectFactory() {
		return objectFactory;
	}

	public void setObjectFactory(ObjectFactory objectFactory) {
		this.objectFactory = objectFactory;
	}

	public ObjectWrapperFactory getObjectWrapperFactory() {
		return objectWrapperFactory;
	}

	public void setObjectWrapperFactory(ObjectWrapperFactory objectWrapperFactory) {
		this.objectWrapperFactory = objectWrapperFactory;
	}
	
	public List<Interceptor> getInterceptors() {
		return interceptorChain.getInterceptors();
	}

	public LanguageDriverRegistry getLanguageRegistry() {
		return languageRegistry;
	}

	public void setDefaultScriptingLanguage(Class<?> driver) {
		if (driver == null) {
			driver = XMLLanguageDriver.class;
		}
		getLanguageRegistry().setDefaultDriverClass(driver);
	}

	public LanguageDriver getDefaultScriptingLanuageInstance() {
		return languageRegistry.getDefaultDriver();
	}

	//创建元对象
	public MetaObject newMetaObject(Object object) {
		return MetaObject.forObject(object, objectFactory, objectWrapperFactory);
	}

	//创建参数处理器
	public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject,
			BoundSql boundSql) {
		//创建参数处理器
		ParameterHandler parameterHandler = mappedStatement.getLang().createParameterHandler(mappedStatement, parameterObject, boundSql);
		//先经过拦截器插件处理
		parameterHandler = (ParameterHandler) interceptorChain.pluginAll(parameterHandler);
		return parameterHandler;
	}

	//创建结果集处理器
	public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds,
			ParameterHandler parameterHandler, ResultHandler resultHandler, BoundSql boundSql) {
		//创建DefaultResultSetHandler(稍老一点的版本3.1是创建NestedResultSetHandler或者FastResultSetHandler)
		ResultSetHandler resultSetHandler = new DefaultResultSetHandler(executor, mappedStatement, parameterHandler,
				resultHandler, boundSql, rowBounds);
		//插件在这里插入
		resultSetHandler = (ResultSetHandler) interceptorChain.pluginAll(resultSetHandler);
		return resultSetHandler;
	}

	//创建语句处理器
	public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement,
			Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
		//创建路由选择语句处理器
		StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
		//先经过拦截器插件处理再返回
		statementHandler = (StatementHandler) interceptorChain.pluginAll(statementHandler);
		return statementHandler;
	}

	//生成执行器
	public Executor newExecutor(Transaction transaction) {
		return newExecutor(transaction, defaultExecutorType);
	}

	//生成执行器
	public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
		//如果执行器类型为空的话就使用默认的执行器类型
		executorType = executorType == null ? defaultExecutorType : executorType;
		executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
		Executor executor;
		//根据执行器类型生成不同的执行器
		if (ExecutorType.BATCH == executorType) {
			executor = new BatchExecutor(this, transaction);
		} else if (ExecutorType.REUSE == executorType) {
			executor = new ReuseExecutor(this, transaction);
		} else {
			executor = new SimpleExecutor(this, transaction);
		}
		//如果要求缓存则返回CachingExecutor
		if (cacheEnabled) {
			executor = new CachingExecutor(executor);
		}
		//此处调用插件,通过插件可以改变Executor行为
		executor = (Executor) interceptorChain.pluginAll(executor);
		return executor;
	}

	//添加KeyGenerator
	public void addKeyGenerator(String id, KeyGenerator keyGenerator) {
		keyGenerators.put(id, keyGenerator);
	}

	//获取KeyGenerator名称集合
	public Collection<String> getKeyGeneratorNames() {
		return keyGenerators.keySet();
	}

	//获取KeyGenerator集合
	public Collection<KeyGenerator> getKeyGenerators() {
		return keyGenerators.values();
	}

	//根据id获取KeyGenerator
	public KeyGenerator getKeyGenerator(String id) {
		return keyGenerators.get(id);
	}

	//是否含有指定id的KeyGenerator
	public boolean hasKeyGenerator(String id) {
		return keyGenerators.containsKey(id);
	}

	//添加缓存
	public void addCache(Cache cache) {
		caches.put(cache.getId(), cache);
	}

	//获取缓存名称集合
	public Collection<String> getCacheNames() {
		return caches.keySet();
	}

	//获取缓存集合
	public Collection<Cache> getCaches() {
		return caches.values();
	}

	//根据id获取缓存
	public Cache getCache(String id) {
		return caches.get(id);
	}

	//是否含有指定id的缓存
	public boolean hasCache(String id) {
		return caches.containsKey(id);
	}

	//添加ResultMap
	public void addResultMap(ResultMap rm) {
		resultMaps.put(rm.getId(), rm);
		checkLocallyForDiscriminatedNestedResultMaps(rm);
		checkGloballyForDiscriminatedNestedResultMaps(rm);
	}

	//获取ResultMap的id集合
	public Collection<String> getResultMapNames() {
		return resultMaps.keySet();
	}

	//获取ResultMap集合
	public Collection<ResultMap> getResultMaps() {
		return resultMaps.values();
	}

	//获取指定id的ResultMap
	public ResultMap getResultMap(String id) {
		return resultMaps.get(id);
	}

	//是否含有指定id的ResultMap
	public boolean hasResultMap(String id) {
		return resultMaps.containsKey(id);
	}

	//添加ParameterMap
	public void addParameterMap(ParameterMap pm) {
		parameterMaps.put(pm.getId(), pm);
	}

	//获取ParameterMap名称集合
	public Collection<String> getParameterMapNames() {
		return parameterMaps.keySet();
	}

	//获取ParameterMap集合
	public Collection<ParameterMap> getParameterMaps() {
		return parameterMaps.values();
	}

	//根据id获取ParameterMap
	public ParameterMap getParameterMap(String id) {
		return parameterMaps.get(id);
	}

	//是否含有指定id的ParameterMap
	public boolean hasParameterMap(String id) {
		return parameterMaps.containsKey(id);
	}

	//添加MappedStatement
	public void addMappedStatement(MappedStatement ms) {
		mappedStatements.put(ms.getId(), ms);
	}

	//获取MappedStatement名称集合
	public Collection<String> getMappedStatementNames() {
		buildAllStatements();
		return mappedStatements.keySet();
	}

	//获取MappedStatement集合
	public Collection<MappedStatement> getMappedStatements() {
		buildAllStatements();
		return mappedStatements.values();
	}

	public Collection<XMLStatementBuilder> getIncompleteStatements() {
		return incompleteStatements;
	}

	public void addIncompleteStatement(XMLStatementBuilder incompleteStatement) {
		incompleteStatements.add(incompleteStatement);
	}

	public Collection<CacheRefResolver> getIncompleteCacheRefs() {
		return incompleteCacheRefs;
	}

	public void addIncompleteCacheRef(CacheRefResolver incompleteCacheRef) {
		incompleteCacheRefs.add(incompleteCacheRef);
	}

	public Collection<ResultMapResolver> getIncompleteResultMaps() {
		return incompleteResultMaps;
	}

	public void addIncompleteResultMap(ResultMapResolver resultMapResolver) {
		incompleteResultMaps.add(resultMapResolver);
	}

	public void addIncompleteMethod(MethodResolver builder) {
		incompleteMethods.add(builder);
	}

	public Collection<MethodResolver> getIncompleteMethods() {
		return incompleteMethods;
	}

	//由DefaultSqlSession.selectList调用过来
	public MappedStatement getMappedStatement(String id) {
		return this.getMappedStatement(id, true);
	}

	public MappedStatement getMappedStatement(String id, boolean validateIncompleteStatements) {
		//先构建所有语句，再返回语句
		if (validateIncompleteStatements) {
			buildAllStatements();
		}
		return mappedStatements.get(id);
	}

	//获取sql片段映射
	public Map<String, XNode> getSqlFragments() {
		return sqlFragments;
	}

	//添加拦截器
	public void addInterceptor(Interceptor interceptor) {
		interceptorChain.addInterceptor(interceptor);
	}

	//注册mapper(指定包名和父类型)
	public void addMappers(String packageName, Class<?> superType) {
		mapperRegistry.addMappers(packageName, superType);
	}

	//注册mapper(指定包名)
	public void addMappers(String packageName) {
		mapperRegistry.addMappers(packageName);
	}

	//注册mapper(指定类型)
	public <T> void addMapper(Class<T> type) {
		mapperRegistry.addMapper(type);
	}

	//根据类型和sqlSession获取mapper
	public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
		return mapperRegistry.getMapper(type, sqlSession);
	}

	//是否存在指定类型的mapper
	public boolean hasMapper(Class<?> type) {
		return mapperRegistry.hasMapper(type);
	}

	public boolean hasStatement(String statementName) {
		return hasStatement(statementName, true);
	}

	public boolean hasStatement(String statementName, boolean validateIncompleteStatements) {
		if (validateIncompleteStatements) {
			buildAllStatements();
		}
		return mappedStatements.containsKey(statementName);
	}

	//添加缓存关系
	public void addCacheRef(String namespace, String referencedNamespace) {
		cacheRefMap.put(namespace, referencedNamespace);
	}

	//构建所有Statement
	protected void buildAllStatements() {
		if (!incompleteResultMaps.isEmpty()) {
			synchronized (incompleteResultMaps) {
				incompleteResultMaps.iterator().next().resolve();
			}
		}
		if (!incompleteCacheRefs.isEmpty()) {
			synchronized (incompleteCacheRefs) {
				incompleteCacheRefs.iterator().next().resolveCacheRef();
			}
		}
		if (!incompleteStatements.isEmpty()) {
			synchronized (incompleteStatements) {
				incompleteStatements.iterator().next().parseStatementNode();
			}
		}
		if (!incompleteMethods.isEmpty()) {
			synchronized (incompleteMethods) {
				incompleteMethods.iterator().next().resolve();
			}
		}
	}
	
	//选取Namespace
	protected String extractNamespace(String statementId) {
		int lastPeriod = statementId.lastIndexOf('.');
		return lastPeriod > 0 ? statementId.substring(0, lastPeriod) : null;
	}
	
	
	protected void checkGloballyForDiscriminatedNestedResultMaps(ResultMap rm) {
		if (rm.hasNestedResultMaps()) {
			for (Map.Entry<String, ResultMap> entry : resultMaps.entrySet()) {
				Object value = entry.getValue();
				if (value instanceof ResultMap) {
					ResultMap entryResultMap = (ResultMap) value;
					if (!entryResultMap.hasNestedResultMaps() && entryResultMap.getDiscriminator() != null) {
						Collection<String> discriminatedResultMapNames = entryResultMap.getDiscriminator()
								.getDiscriminatorMap().values();
						if (discriminatedResultMapNames.contains(rm.getId())) {
							entryResultMap.forceNestedResultMaps();
						}
					}
				}
			}
		}
	}
	
	
	protected void checkLocallyForDiscriminatedNestedResultMaps(ResultMap rm) {
		if (!rm.hasNestedResultMaps() && rm.getDiscriminator() != null) {
			for (Map.Entry<String, String> entry : rm.getDiscriminator().getDiscriminatorMap().entrySet()) {
				String discriminatedResultMapName = entry.getValue();
				if (hasResultMap(discriminatedResultMapName)) {
					ResultMap discriminatedResultMap = resultMaps.get(discriminatedResultMapName);
					if (discriminatedResultMap.hasNestedResultMaps()) {
						rm.forceNestedResultMaps();
						break;
					}
				}
			}
		}
	}

	//静态内部类,严格的Map,不允许多次覆盖key所对应的value
	protected static class StrictMap<V> extends HashMap<String, V> {

		private static final long serialVersionUID = -4950446264854982944L;
		private String name;

		public StrictMap(String name, int initialCapacity, float loadFactor) {
			super(initialCapacity, loadFactor);
			this.name = name;
		}

		public StrictMap(String name, int initialCapacity) {
			super(initialCapacity);
			this.name = name;
		}

		public StrictMap(String name) {
			super();
			this.name = name;
		}

		public StrictMap(String name, Map<String, ? extends V> m) {
			super(m);
			this.name = name;
		}

		@SuppressWarnings("unchecked")
		public V put(String key, V value) {
			//若key已存在则报错
			if (containsKey(key)) {
				throw new IllegalArgumentException(name + " already contains value for " + key);
			}
			//若key包含"."
			if (key.contains(".")) {
				//获取短名称
				final String shortKey = getShortName(key);
				if (super.get(shortKey) == null) {
					//如果没有这个缩略，则放一个缩略
					super.put(shortKey, value);
				} else {
					//如果已经有此缩略，表示模糊，放一个Ambiguity型的
					super.put(shortKey, (V) new Ambiguity(shortKey));
				}
			}
			//再放一个全名
			return super.put(key, value);
			//可以看到，如果有包名，会放2个key到这个map，一个缩略，一个全名
		}

		public V get(Object key) {
			V value = super.get(key);
			if (value == null) {
				throw new IllegalArgumentException(name + " does not contain value for " + key);
			}
			if (value instanceof Ambiguity) {
				throw new IllegalArgumentException(((Ambiguity) value).getSubject() + " is ambiguous in " + name
						+ " (try using the full name including the namespace, or rename one of the entries)");
			}
			return value;
		}

		//取得短名称，也就是取得最后那个句号的后面那部分
		private String getShortName(String key) {
			final String[] keyparts = key.split("\\.");
			return keyparts[keyparts.length - 1];
		}

		//模糊，居然放在Map里面的一个静态内部类，
		protected static class Ambiguity {
			//提供一个主题
			private String subject;

			public Ambiguity(String subject) {
				this.subject = subject;
			}

			public String getSubject() {
				return subject;
			}
		}
	}

}
