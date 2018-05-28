package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

//mapper文件解析器
public class XMLMapperBuilder extends BaseBuilder {

	private XPathParser parser;                        //XPath解析器
	private MapperBuilderAssistant builderAssistant;   //构建助手
	private Map<String, XNode> sqlFragments;           //xml片段集合
	private String resource;                           //mapper.xml文件资源路径

	//构造器1
	public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource,
			Map<String, XNode> sqlFragments, String namespace) {
		this(inputStream, configuration, resource, sqlFragments);
		this.builderAssistant.setCurrentNamespace(namespace);
	}

	//构造器2
	public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource,
			Map<String, XNode> sqlFragments) {
		this(new XPathParser(inputStream, true, configuration.getVariables(), new XMLMapperEntityResolver()),
				configuration, resource, sqlFragments);
	}

	//构造器3
	private XMLMapperBuilder(XPathParser parser, Configuration configuration, String resource,
			Map<String, XNode> sqlFragments) {
		super(configuration);
		this.builderAssistant = new MapperBuilderAssistant(configuration, resource);
		this.parser = parser;
		this.resource = resource;
		this.sqlFragments = sqlFragments;
	}

	//解析方法
	public void parse() {
		//判断资源是否已经加载过
		if (!configuration.isResourceLoaded(resource)) {
			//解析<mapper>节点
			configurationElement(parser.evalNode("/mapper"));
			//将该资源标记为已加载
			configuration.addLoadedResource(resource);
			//将mapper绑定到namespace
			bindMapperForNamespace();
		}
		
		//解析待定的结果集映射
		parsePendingResultMaps();
		//解析待定的缓存关系
		parsePendingChacheRefs();
		//解析待定的语句
		parsePendingStatements();
	}

	//解析<mapper>
	private void configurationElement(XNode context) {
		try {
			String namespace = context.getStringAttribute("namespace");
			if (namespace.equals("")) {
				throw new BuilderException("Mapper's namespace cannot be empty");
			}
			//设置mapper名称空间
			builderAssistant.setCurrentNamespace(namespace);
			//1.解析<cache-ref>
			cacheRefElement(context.evalNode("cache-ref"));
			//2.解析<cache>
			cacheElement(context.evalNode("cache"));
			//3.解析<parameterMap>(已经废弃,老式风格的参数映射)
			parameterMapElement(context.evalNodes("/mapper/parameterMap"));
			//4.解析<resultMap>
			resultMapElements(context.evalNodes("/mapper/resultMap"));
			//5.解析<sql>
			sqlElement(context.evalNodes("/mapper/sql"));
			//6.解析select|insert|update|delete
			buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
		} catch (Exception e) {
			throw new BuilderException("Error parsing Mapper XML. Cause: " + e, e);
		}
	}

	//1.解析<cache-ref>
	private void cacheRefElement(XNode context) {
		if (context != null) {
			//添加到缓存关系映射
			configuration.addCacheRef(builderAssistant.getCurrentNamespace(), context.getStringAttribute("namespace"));
			//新建缓存映射解析器
			CacheRefResolver cacheRefResolver = new CacheRefResolver(builderAssistant, context.getStringAttribute("namespace"));
			try {
				//解析缓存映射
				cacheRefResolver.resolveCacheRef();
			} catch (IncompleteElementException e) {
				//出现异常则添加到待处理的缓存关系集合中
				configuration.addIncompleteCacheRef(cacheRefResolver);
			}
		}
	}

	//2.解析<cache>
	private void cacheElement(XNode context) throws Exception {
		if (context != null) {
			//获取缓存类型别名
			String type = context.getStringAttribute("type", "PERPETUAL");
			//获取缓存类型
			Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
			//获取缓存移除方法别名
			String eviction = context.getStringAttribute("eviction", "LRU");
			//获取缓存移除方法类
			Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
			//获取刷新间隔
			Long flushInterval = context.getLongAttribute("flushInterval");
			//获取缓存大小
			Integer size = context.getIntAttribute("size");
			//是否读写
			boolean readWrite = !context.getBooleanAttribute("readOnly", false);
			//是否按块
			boolean blocking = context.getBooleanAttribute("blocking", false);
			//读入额外的配置信息, 易于第三方的缓存扩展
			Properties props = context.getChildrenAsProperties();
			//使用构建助手进行处理
			builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
		}
	}

	//3.解析<parameterMap>
	private void parameterMapElement(List<XNode> list) throws Exception {
		for (XNode parameterMapNode : list) {
			String id = parameterMapNode.getStringAttribute("id");
			String type = parameterMapNode.getStringAttribute("type");
			Class<?> parameterClass = resolveClass(type);
			List<XNode> parameterNodes = parameterMapNode.evalNodes("parameter");
			List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();
			for (XNode parameterNode : parameterNodes) {
				String property = parameterNode.getStringAttribute("property");
				String javaType = parameterNode.getStringAttribute("javaType");
				String jdbcType = parameterNode.getStringAttribute("jdbcType");
				String resultMap = parameterNode.getStringAttribute("resultMap");
				String mode = parameterNode.getStringAttribute("mode");
				String typeHandler = parameterNode.getStringAttribute("typeHandler");
				Integer numericScale = parameterNode.getIntAttribute("numericScale");
				ParameterMode modeEnum = resolveParameterMode(mode);
				Class<?> javaTypeClass = resolveClass(javaType);
				JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
				@SuppressWarnings("unchecked")
				Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
				ParameterMapping parameterMapping = builderAssistant.buildParameterMapping(parameterClass, property,
						javaTypeClass, jdbcTypeEnum, resultMap, modeEnum, typeHandlerClass, numericScale);
				parameterMappings.add(parameterMapping);
			}
			builderAssistant.addParameterMap(id, parameterClass, parameterMappings);
		}
	}

	//4.解析<resultMap>
	private void resultMapElements(List<XNode> list) throws Exception {
		for (XNode resultMapNode : list) {
			try {
				resultMapElement(resultMapNode);
			} catch (IncompleteElementException e) {
				//ignore
			}
		}
	}

	//4.1 解析<resultMap>
	private ResultMap resultMapElement(XNode resultMapNode) throws Exception {
		return resultMapElement(resultMapNode, Collections.<ResultMapping>emptyList());
	}

	//4.2 解析<resultMap>
	private ResultMap resultMapElement(XNode resultMapNode, List<ResultMapping> additionalResultMappings)
			throws Exception {
		//设置错误上下文
		ErrorContext.instance().activity("processing " + resultMapNode.getValueBasedIdentifier());
		//获取id属性值
		String id = resultMapNode.getStringAttribute("id", resultMapNode.getValueBasedIdentifier());
		//获取type属性值
		String type = resultMapNode.getStringAttribute("type", resultMapNode.getStringAttribute("ofType",
				resultMapNode.getStringAttribute("resultType", resultMapNode.getStringAttribute("javaType"))));
		//获取extends属性值
		String extend = resultMapNode.getStringAttribute("extends");
		//获取autoMapping属性值
		Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");
		//获取type属性对应的类
		Class<?> typeClass = resolveClass(type);
		Discriminator discriminator = null;
		List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
		//添加额外的ResultMapping
		resultMappings.addAll(additionalResultMappings);
		List<XNode> resultChildren = resultMapNode.getChildren();
		//遍历子节点
		for (XNode resultChild : resultChildren) {
			//解析<constructor>
			if ("constructor".equals(resultChild.getName())) {
				processConstructorElement(resultChild, typeClass, resultMappings);
		    //解析<discriminator>
			} else if ("discriminator".equals(resultChild.getName())) {
				discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
			//解析<id>和<result>
			} else {
				List<ResultFlag> flags = new ArrayList<ResultFlag>();
				//若是<id>节点, 则添加到flags中
				if ("id".equals(resultChild.getName())) {
					flags.add(ResultFlag.ID);
				}
				//构建ResultMapping并添加到列表中
				resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
			}
		}
		//新建结果集映射转换器
		ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend,
				discriminator, resultMappings, autoMapping);
		try {
			//调用转换器的转换方法
			return resultMapResolver.resolve();
		} catch (IncompleteElementException e) {
			//出现异常则添加到待处理的结果映射集合中
			configuration.addIncompleteResultMap(resultMapResolver);
			throw e;
		}
	}

	//4.3 解析<constructor>
	private void processConstructorElement(XNode resultChild, Class<?> resultType, List<ResultMapping> resultMappings)
			throws Exception {
		List<XNode> argChildren = resultChild.getChildren();
		for (XNode argChild : argChildren) {
			List<ResultFlag> flags = new ArrayList<ResultFlag>();
			//结果标志加上ID和CONSTRUCTOR
			flags.add(ResultFlag.CONSTRUCTOR);
			if ("idArg".equals(argChild.getName())) {
				flags.add(ResultFlag.ID);
			}
			resultMappings.add(buildResultMappingFromContext(argChild, resultType, flags));
		}
	}

	//4.4 解析<discriminator>
	private Discriminator processDiscriminatorElement(XNode context, Class<?> resultType,
			List<ResultMapping> resultMappings) throws Exception {
		String column = context.getStringAttribute("column");
		String javaType = context.getStringAttribute("javaType");
		String jdbcType = context.getStringAttribute("jdbcType");
		String typeHandler = context.getStringAttribute("typeHandler");
		Class<?> javaTypeClass = resolveClass(javaType);
		@SuppressWarnings("unchecked")
		Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
		JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
		Map<String, String> discriminatorMap = new HashMap<String, String>();
		//遍历所有子结点
		for (XNode caseChild : context.getChildren()) {
			//获取value属性值
			String value = caseChild.getStringAttribute("value");
			//获取resultMap属性值
			String resultMap = caseChild.getStringAttribute("resultMap", processNestedResultMappings(caseChild, resultMappings));
			discriminatorMap.put(value, resultMap);
		}
		return builderAssistant.buildDiscriminator(resultType, column, javaTypeClass, jdbcTypeEnum, typeHandlerClass, discriminatorMap);
	}
	
	//4.5 构建ResultMapping
	private ResultMapping buildResultMappingFromContext(XNode context, Class<?> resultType, List<ResultFlag> flags)
			throws Exception {
		//获取各种属性值
		String property = context.getStringAttribute("property");
		String column = context.getStringAttribute("column");
		String javaType = context.getStringAttribute("javaType");
		String jdbcType = context.getStringAttribute("jdbcType");
		String nestedSelect = context.getStringAttribute("select");
		//处理嵌套的resultMap
		String nestedResultMap = context.getStringAttribute("resultMap", processNestedResultMappings(context, Collections.<ResultMapping>emptyList()));
		String notNullColumn = context.getStringAttribute("notNullColumn");
		String columnPrefix = context.getStringAttribute("columnPrefix");
		String typeHandler = context.getStringAttribute("typeHandler");
		String resulSet = context.getStringAttribute("resultSet");
		String foreignColumn = context.getStringAttribute("foreignColumn");
		//判断是否懒加载
		boolean lazy = "lazy".equals(context.getStringAttribute("fetchType", configuration.isLazyLoadingEnabled() ? "lazy" : "eager"));
		Class<?> javaTypeClass = resolveClass(javaType);
		@SuppressWarnings("unchecked")
		Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
		JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
		//使用构建助手构建ResultMapping
		return builderAssistant.buildResultMapping(resultType, property, column, javaTypeClass, jdbcTypeEnum,
				nestedSelect, nestedResultMap, notNullColumn, columnPrefix, typeHandlerClass, flags, resulSet,
				foreignColumn, lazy);
	}
	
	//4.6 处理嵌套ResultMapping
	private String processNestedResultMappings(XNode context, List<ResultMapping> resultMappings) throws Exception {
		//处理association|collection|case
		if ("association".equals(context.getName()) || "collection".equals(context.getName())
				|| "case".equals(context.getName())) {
			if (context.getStringAttribute("select") == null) {
				ResultMap resultMap = resultMapElement(context, resultMappings);
				return resultMap.getId();
			}
		}
		return null;
	}

	//5.解析<sql>
	private void sqlElement(List<XNode> list) throws Exception {
		if (configuration.getDatabaseId() != null) {
			sqlElement(list, configuration.getDatabaseId());
		}
		sqlElement(list, null);
	}

	//5.1 解析<sql>
	private void sqlElement(List<XNode> list, String requiredDatabaseId) throws Exception {
		for (XNode context : list) {
			String databaseId = context.getStringAttribute("databaseId");
			String id = context.getStringAttribute("id");
			id = builderAssistant.applyCurrentNamespace(id, false);
			if (databaseIdMatchesCurrent(id, databaseId, requiredDatabaseId)) {
				sqlFragments.put(id, context);
			}
		}
	}

	//5.2 解析<sql>
	private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
		if (requiredDatabaseId != null) {
			if (!requiredDatabaseId.equals(databaseId)) {
				return false;
			}
		} else {
			if (databaseId != null) {
				return false;
			}
			if (this.sqlFragments.containsKey(id)) {
				XNode context = this.sqlFragments.get(id);
				//如果之前那个重名的sql id有databaseId，则false，否则难道true？这样新的sql覆盖老的sql？？？
				if (context.getStringAttribute("databaseId") != null) {
					return false;
				}
			}
		}
		return true;
	}
	
	//6.解析select|insert|update|delete
	private void buildStatementFromContext(List<XNode> list) {
		if (configuration.getDatabaseId() != null) {
			buildStatementFromContext(list, configuration.getDatabaseId());
		}
		buildStatementFromContext(list, null);
	}

	//6.1 构建语句
	private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
		for (XNode context : list) {
			//新建XML语句构建者
			final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant,
					context, requiredDatabaseId);
			try {
				//调用构建方法进行构建
				statementParser.parseStatementNode();
			} catch (IncompleteElementException e) {
				//出现异常则添加到待处理的语句集合中
				configuration.addIncompleteStatement(statementParser);
			}
		}
	}

	//解析待定的结果集映射
	private void parsePendingResultMaps() {
		//获取不完全的结果映射集合
		Collection<ResultMapResolver> incompleteResultMaps = configuration.getIncompleteResultMaps();
		synchronized (incompleteResultMaps) {
			Iterator<ResultMapResolver> iter = incompleteResultMaps.iterator();
			//遍历迭代器, 再次对集合元素做一次处理
			while (iter.hasNext()) {
				try {
					//调用结果集转换器的转换方法
					iter.next().resolve();
					//然后将其删除
					iter.remove();
				} catch (IncompleteElementException e) {
					//出现异常不处理
				}
			}
		}
	}

	//解析待定的缓存关系
	private void parsePendingChacheRefs() {
		Collection<CacheRefResolver> incompleteCacheRefs = configuration.getIncompleteCacheRefs();
		synchronized (incompleteCacheRefs) {
			Iterator<CacheRefResolver> iter = incompleteCacheRefs.iterator();
			//遍历迭代器, 再次对集合元素做一次处理
			while (iter.hasNext()) {
				try {
					//调用缓存关系转换器的转换方法
					iter.next().resolveCacheRef();
					//然后将其删除
					iter.remove();
				} catch (IncompleteElementException e) {
					//出现异常不处理
				}
			}
		}
	}

	//解析待定的语句
	private void parsePendingStatements() {
		Collection<XMLStatementBuilder> incompleteStatements = configuration.getIncompleteStatements();
		synchronized (incompleteStatements) {
			Iterator<XMLStatementBuilder> iter = incompleteStatements.iterator();
			//遍历迭代器, 再次对集合元素做一次处理
			while (iter.hasNext()) {
				try {
					//调用语句构建器进行构建
					iter.next().parseStatementNode();
					//然后将其删除
					iter.remove();
				} catch (IncompleteElementException e) {
					//出现异常不处理
				}
			}
		}
	}
	
	//绑定mapper到名称空间
	private void bindMapperForNamespace() {
		//获取当前名称空间
		String namespace = builderAssistant.getCurrentNamespace();
		if (namespace != null) {
			Class<?> boundType = null;
			try {
				//获取名称空间对应的Class类型
				boundType = Resources.classForName(namespace);
			} catch (ClassNotFoundException e) {
				// ignore
			}
			if (boundType != null) {
				//如果配置信息不包含该Mapper
				if (!configuration.hasMapper(boundType)) {
					//添加已加载资源文件
					configuration.addLoadedResource("namespace:" + namespace);
					//添加Mapper类型
					configuration.addMapper(boundType);
				}
			}
		}
	}
	
	//获取SQL片段
	public XNode getSqlFragment(String refid) {
		return sqlFragments.get(refid);
	}

}
