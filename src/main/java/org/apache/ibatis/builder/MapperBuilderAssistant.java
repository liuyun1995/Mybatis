package org.apache.ibatis.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.CacheBuilder;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

//Mapper构建器助手
public class MapperBuilderAssistant extends BaseBuilder {
	
	private String currentNamespace;
	private String resource;
	private Cache currentCache;
	private boolean unresolvedCacheRef;

	//构造方法
	public MapperBuilderAssistant(Configuration configuration, String resource) {
		super(configuration);
		ErrorContext.instance().resource(resource);
		this.resource = resource;
	}

	//获取当前Namespace
	public String getCurrentNamespace() {
		return currentNamespace;
	}

	//设置当前Namespace
	public void setCurrentNamespace(String currentNamespace) {
		if (currentNamespace == null) {
			throw new BuilderException("The mapper element requires a namespace attribute to be specified.");
		}
		if (this.currentNamespace != null && !this.currentNamespace.equals(currentNamespace)) {
			throw new BuilderException(
					"Wrong namespace. Expected '" + this.currentNamespace + "' but found '" + currentNamespace + "'.");
		}
		this.currentNamespace = currentNamespace;
	}

	//加上当前namespace前缀
	public String applyCurrentNamespace(String base, boolean isReference) {
		if (base == null) {
			return null;
		}
		if (isReference) {
			if (base.contains(".")) {
				return base;
			}
		} else {
			if (base.startsWith(currentNamespace + ".")) {
				return base;
			}
			if (base.contains(".")) {
				throw new BuilderException("Dots are not allowed in element names, please remove it from " + base);
			}
		}
		return currentNamespace + "." + base;
	}

	//使用指定namespace的缓存
	public Cache useCacheRef(String namespace) {
		if (namespace == null) {
			throw new BuilderException("cache-ref element requires a namespace attribute.");
		}
		try {
			unresolvedCacheRef = true;
			//根据namespace获取缓存
			Cache cache = configuration.getCache(namespace);
			if (cache == null) {
				throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.");
			}
			//根据当前缓存
			currentCache = cache;
			unresolvedCacheRef = false;
			return cache;
		} catch (IllegalArgumentException e) {
			throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.", e);
		}
	}

	//使用新缓存
	public Cache useNewCache(Class<? extends Cache> typeClass, Class<? extends Cache> evictionClass, Long flushInterval,
			Integer size, boolean readWrite, boolean blocking, Properties props) {
		//再次验证是否为null, 否则就用默认值
		typeClass = valueOrDefault(typeClass, PerpetualCache.class);
		evictionClass = valueOrDefault(evictionClass, LruCache.class);
		//使用CacheBuilder构建cache
		Cache cache = new CacheBuilder(currentNamespace).implementation(typeClass).addDecorator(evictionClass)
				.clearInterval(flushInterval).size(size).readWrite(readWrite).blocking(blocking).properties(props)
				.build();
		//将该缓存加入到配置中
		configuration.addCache(cache);
		//设置当前的缓存
		currentCache = cache;
		return cache;
	}

	//添加ParameterMap
	public ParameterMap addParameterMap(String id, Class<?> parameterClass, List<ParameterMapping> parameterMappings) {
		id = applyCurrentNamespace(id, false);
		ParameterMap.Builder parameterMapBuilder = new ParameterMap.Builder(configuration, id, parameterClass,
				parameterMappings);
		ParameterMap parameterMap = parameterMapBuilder.build();
		configuration.addParameterMap(parameterMap);
		return parameterMap;
	}

	//构建ParameterMapping
	public ParameterMapping buildParameterMapping(Class<?> parameterType, String property, Class<?> javaType,
			JdbcType jdbcType, String resultMap, ParameterMode parameterMode,
			Class<? extends TypeHandler<?>> typeHandler, Integer numericScale) {
		resultMap = applyCurrentNamespace(resultMap, true);
		Class<?> javaTypeClass = resolveParameterJavaType(parameterType, property, javaType, jdbcType);
		TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, typeHandler);
		ParameterMapping.Builder builder = new ParameterMapping.Builder(configuration, property, javaTypeClass);
		builder.jdbcType(jdbcType);
		builder.resultMapId(resultMap);
		builder.mode(parameterMode);
		builder.numericScale(numericScale);
		builder.typeHandler(typeHandlerInstance);
		return builder.build();
	}

	//添加ResultMap
	public ResultMap addResultMap(String id, Class<?> type, String extend, Discriminator discriminator,
			List<ResultMapping> resultMappings, Boolean autoMapping) {
		id = applyCurrentNamespace(id, false);
		extend = applyCurrentNamespace(extend, true);
		//建造者模式
		ResultMap.Builder resultMapBuilder = new ResultMap.Builder(configuration, id, type, resultMappings, autoMapping);
		if (extend != null) {
			//如果没有extend对应的ResultMap, 则抛出异常
			if (!configuration.hasResultMap(extend)) {
				throw new IncompleteElementException("Could not find a parent resultmap with id '" + extend + "'");
			}
			//获取extend对应的ResultMap
			ResultMap resultMap = configuration.getResultMap(extend);
			//获取extend的ResultMapping集合
			List<ResultMapping> extendedResultMappings = new ArrayList<ResultMapping>(resultMap.getResultMappings());
			//移除重复的resultMappings
			extendedResultMappings.removeAll(resultMappings);
			// Remove parent constructor if this resultMap declares a constructor.
			boolean declaresConstructor = false;
			for (ResultMapping resultMapping : resultMappings) {
				if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
					declaresConstructor = true;
					break;
				}
			}
			if (declaresConstructor) {
				Iterator<ResultMapping> extendedResultMappingsIter = extendedResultMappings.iterator();
				while (extendedResultMappingsIter.hasNext()) {
					if (extendedResultMappingsIter.next().getFlags().contains(ResultFlag.CONSTRUCTOR)) {
						extendedResultMappingsIter.remove();
					}
				}
			}
			//添加额外的ResultMapping
			resultMappings.addAll(extendedResultMappings);
		}
		resultMapBuilder.discriminator(discriminator);
		ResultMap resultMap = resultMapBuilder.build();
		configuration.addResultMap(resultMap);
		return resultMap;
	}

	//构建Discriminator
	public Discriminator buildDiscriminator(Class<?> resultType, String column, Class<?> javaType, JdbcType jdbcType,
			Class<? extends TypeHandler<?>> typeHandler, Map<String, String> discriminatorMap) {
		ResultMapping resultMapping = buildResultMapping(resultType, null, column, javaType, jdbcType, null, null, null,
				null, typeHandler, new ArrayList<ResultFlag>(), null, null, false);
		Map<String, String> namespaceDiscriminatorMap = new HashMap<String, String>();
		for (Map.Entry<String, String> e : discriminatorMap.entrySet()) {
			String resultMap = e.getValue();
			resultMap = applyCurrentNamespace(resultMap, true);
			namespaceDiscriminatorMap.put(e.getKey(), resultMap);
		}
		Discriminator.Builder discriminatorBuilder = new Discriminator.Builder(configuration, resultMapping,
				namespaceDiscriminatorMap);
		return discriminatorBuilder.build();
	}

	//添加MappedStatement
	public MappedStatement addMappedStatement(String id, SqlSource sqlSource, StatementType statementType,
			SqlCommandType sqlCommandType, Integer fetchSize, Integer timeout, String parameterMap,
			Class<?> parameterType, String resultMap, Class<?> resultType, ResultSetType resultSetType,
			boolean flushCache, boolean useCache, boolean resultOrdered, KeyGenerator keyGenerator, String keyProperty,
			String keyColumn, String databaseId, LanguageDriver lang, String resultSets) {
		
		if (unresolvedCacheRef) {
			throw new IncompleteElementException("Cache-ref not yet resolved");
		}
		//为id加上namespace前缀
		id = applyCurrentNamespace(id, false);
		//是否是select语句
		boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
		
		MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, id, sqlSource, sqlCommandType);
		statementBuilder.resource(resource);
		statementBuilder.fetchSize(fetchSize);
		statementBuilder.statementType(statementType);
		statementBuilder.keyGenerator(keyGenerator);
		statementBuilder.keyProperty(keyProperty);
		statementBuilder.keyColumn(keyColumn);
		statementBuilder.databaseId(databaseId);
		statementBuilder.lang(lang);
		statementBuilder.resultOrdered(resultOrdered);
		statementBuilder.resulSets(resultSets);
		//设置超时时间
		setStatementTimeout(timeout, statementBuilder);
		//设置ParameterMap
		setStatementParameterMap(parameterMap, parameterType, statementBuilder);
		//设置ResultMap
		setStatementResultMap(resultMap, resultType, resultSetType, statementBuilder);
		//设置语句缓存
		setStatementCache(isSelect, flushCache, useCache, currentCache, statementBuilder);
		//进行构建
		MappedStatement statement = statementBuilder.build();
		//添加到配置信息中
		configuration.addMappedStatement(statement);
		return statement;
	}

	//设置超时时间
	private void setStatementTimeout(Integer timeout, MappedStatement.Builder statementBuilder) {
		if (timeout == null) {
			//获取默认超时时间
			timeout = configuration.getDefaultStatementTimeout();
		}
		statementBuilder.timeout(timeout);
	}

	//设置ParameterMap
	private void setStatementParameterMap(String parameterMap, Class<?> parameterTypeClass,
			MappedStatement.Builder statementBuilder) {
		parameterMap = applyCurrentNamespace(parameterMap, true);
		if (parameterMap != null) {
			try {
				statementBuilder.parameterMap(configuration.getParameterMap(parameterMap));
			} catch (IllegalArgumentException e) {
				throw new IncompleteElementException("Could not find parameter map " + parameterMap, e);
			}
		} else if (parameterTypeClass != null) {
			List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();
			ParameterMap.Builder inlineParameterMapBuilder = new ParameterMap.Builder(configuration,
					statementBuilder.id() + "-Inline", parameterTypeClass, parameterMappings);
			statementBuilder.parameterMap(inlineParameterMapBuilder.build());
		}
	}

	//设置ResultMap
	private void setStatementResultMap(String resultMap, Class<?> resultType, ResultSetType resultSetType,
			MappedStatement.Builder statementBuilder) {
		resultMap = applyCurrentNamespace(resultMap, true);
		List<ResultMap> resultMaps = new ArrayList<ResultMap>();
		if (resultMap != null) {
			String[] resultMapNames = resultMap.split(",");
			for (String resultMapName : resultMapNames) {
				try {
					resultMaps.add(configuration.getResultMap(resultMapName.trim()));
				} catch (IllegalArgumentException e) {
					throw new IncompleteElementException("Could not find result map " + resultMapName, e);
				}
			}
		} else if (resultType != null) {
			// 2.2 resultType,一般用这个足矣了
			// <select id="selectUsers" resultType="User">
			// 这种情况下,MyBatis 会在幕后自动创建一个 ResultMap,基于属性名来映射列到 JavaBean的属性上。
			// 如果列名没有精确匹配,你可以在列名上使用 select 字句的别名来匹配标签。
			// 创建一个inline result map, 把resultType设上就OK了，
			// 然后后面被DefaultResultSetHandler.createResultObject()使用
			// DefaultResultSetHandler.getRowValue()使用
			ResultMap.Builder inlineResultMapBuilder = new ResultMap.Builder(configuration,
					statementBuilder.id() + "-Inline", resultType, new ArrayList<ResultMapping>(), null);
			resultMaps.add(inlineResultMapBuilder.build());
		}
		statementBuilder.resultMaps(resultMaps);
		statementBuilder.resultSetType(resultSetType);
	}
	
	//设置语句缓存
	private void setStatementCache(boolean isSelect, boolean flushCache, boolean useCache, Cache cache,
			MappedStatement.Builder statementBuilder) {
		flushCache = valueOrDefault(flushCache, !isSelect);
		useCache = valueOrDefault(useCache, isSelect);
		statementBuilder.flushCacheRequired(flushCache);
		statementBuilder.useCache(useCache);
		statementBuilder.cache(cache);
	}

	private <T> T valueOrDefault(T value, T defaultValue) {
		return value == null ? defaultValue : value;
	}
	
	//构建ResultMapping
	public ResultMapping buildResultMapping(Class<?> resultType, String property, String column, Class<?> javaType,
			JdbcType jdbcType, String nestedSelect, String nestedResultMap, String notNullColumn, String columnPrefix,
			Class<? extends TypeHandler<?>> typeHandler, List<ResultFlag> flags, String resultSet, String foreignColumn,
			boolean lazy) {
		Class<?> javaTypeClass = resolveResultJavaType(resultType, property, javaType);
		TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, typeHandler);
		//解析复合的列名, 一般用不到, 返回的是空
		List<ResultMapping> composites = parseCompositeColumnName(column);
		if (composites.size() > 0) {
			column = null;
		}
		//构建ResultMapping
		ResultMapping.Builder builder = new ResultMapping.Builder(configuration, property, column, javaTypeClass);
		builder.jdbcType(jdbcType);
		builder.nestedQueryId(applyCurrentNamespace(nestedSelect, true));
		builder.nestedResultMapId(applyCurrentNamespace(nestedResultMap, true));
		builder.resultSet(resultSet);
		builder.typeHandler(typeHandlerInstance);
		builder.flags(flags == null ? new ArrayList<ResultFlag>() : flags);
		builder.composites(composites);
		builder.notNullColumns(parseMultipleColumnNames(notNullColumn));
		builder.columnPrefix(columnPrefix);
		builder.foreignColumn(foreignColumn);
		builder.lazy(lazy);
		return builder.build();
	}

	//解析重复列名
	private Set<String> parseMultipleColumnNames(String columnName) {
		Set<String> columns = new HashSet<String>();
		if (columnName != null) {
			if (columnName.indexOf(',') > -1) {
				StringTokenizer parser = new StringTokenizer(columnName, "{}, ", false);
				while (parser.hasMoreTokens()) {
					String column = parser.nextToken();
					columns.add(column);
				}
			} else {
				columns.add(columnName);
			}
		}
		return columns;
	}

	//解析复合列名, 即列名由多个组成, 可以先忽略
	private List<ResultMapping> parseCompositeColumnName(String columnName) {
		List<ResultMapping> composites = new ArrayList<ResultMapping>();
		if (columnName != null && (columnName.indexOf('=') > -1 || columnName.indexOf(',') > -1)) {
			StringTokenizer parser = new StringTokenizer(columnName, "{}=, ", false);
			while (parser.hasMoreTokens()) {
				String property = parser.nextToken();
				String column = parser.nextToken();
				ResultMapping.Builder complexBuilder = new ResultMapping.Builder(configuration, property, column,
						configuration.getTypeHandlerRegistry().getUnknownTypeHandler());
				composites.add(complexBuilder.build());
			}
		}
		return composites;
	}

	//解析返回java类型
	private Class<?> resolveResultJavaType(Class<?> resultType, String property, Class<?> javaType) {
		if (javaType == null && property != null) {
			try {
				MetaClass metaResultType = MetaClass.forClass(resultType);
				javaType = metaResultType.getSetterType(property);
			} catch (Exception e) {
				// ignore
			}
		}
		if (javaType == null) {
			javaType = Object.class;
		}
		return javaType;
	}

	//解析参数java类型
	private Class<?> resolveParameterJavaType(Class<?> resultType, String property, Class<?> javaType,
			JdbcType jdbcType) {
		if (javaType == null) {
			if (JdbcType.CURSOR.equals(jdbcType)) {
				javaType = java.sql.ResultSet.class;
			} else if (Map.class.isAssignableFrom(resultType)) {
				javaType = Object.class;
			} else {
				MetaClass metaResultType = MetaClass.forClass(resultType);
				javaType = metaResultType.getGetterType(property);
			}
		}
		if (javaType == null) {
			javaType = Object.class;
		}
		return javaType;
	}
	
	//向后兼容方法
	public ResultMapping buildResultMapping(Class<?> resultType, String property, String column, Class<?> javaType,
			JdbcType jdbcType, String nestedSelect, String nestedResultMap, String notNullColumn, String columnPrefix,
			Class<? extends TypeHandler<?>> typeHandler, List<ResultFlag> flags) {
		return buildResultMapping(resultType, property, column, javaType, jdbcType, nestedSelect, nestedResultMap,
				notNullColumn, columnPrefix, typeHandler, flags, null, null, configuration.isLazyLoadingEnabled());
	}

	//获取语言驱动
	public LanguageDriver getLanguageDriver(Class<?> langClass) {
		if (langClass != null) {
			//注册语言驱动
			configuration.getLanguageRegistry().register(langClass);
		} else {
			//如果为null，则取得默认驱动（mybatis3.2以前大家一直用的方法）
			langClass = configuration.getLanguageRegistry().getDefaultDriverClass();
		}
		//再去调configuration
		return configuration.getLanguageRegistry().getDriver(langClass);
	}
	
	//向后兼容方法
	public MappedStatement addMappedStatement(String id, SqlSource sqlSource, StatementType statementType,
			SqlCommandType sqlCommandType, Integer fetchSize, Integer timeout, String parameterMap,
			Class<?> parameterType, String resultMap, Class<?> resultType, ResultSetType resultSetType,
			boolean flushCache, boolean useCache, boolean resultOrdered, KeyGenerator keyGenerator, String keyProperty,
			String keyColumn, String databaseId, LanguageDriver lang) {
		return addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap,
				parameterType, resultMap, resultType, resultSetType, flushCache, useCache, resultOrdered, keyGenerator,
				keyProperty, keyColumn, databaseId, lang, null);
	}

}
