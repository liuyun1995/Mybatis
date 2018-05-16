package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;

//config文件解析器
public class XMLConfigBuilder extends BaseBuilder {

	private boolean parsed;     //是否已解析
	private XPathParser parser;  //XPath解析器
	private String environment;  //环境信息
	
	public XMLConfigBuilder(Reader reader) {
		this(reader, null, null);
	}

	public XMLConfigBuilder(Reader reader, String environment) {
		this(reader, environment, null);
	}
	
	public XMLConfigBuilder(Reader reader, String environment, Properties props) {
		this(new XPathParser(reader, true, props, new XMLMapperEntityResolver()), environment, props);
	}
	
	public XMLConfigBuilder(InputStream inputStream) {
		this(inputStream, null, null);
	}

	public XMLConfigBuilder(InputStream inputStream, String environment) {
		this(inputStream, environment, null);
	}

	public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
		this(new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
	}
	
	private XMLConfigBuilder(XPathParser parser, String environment, Properties props) {
		super(new Configuration());
		ErrorContext.instance().resource("SQL Mapper Configuration");
		this.configuration.setVariables(props);
		this.parsed = false;
		this.environment = environment;
		this.parser = parser;
	}

	//解析配置文件核心方法
	public Configuration parse() {
		if (parsed) {
			throw new BuilderException("Each XMLConfigBuilder can only be used once.");
		}
		parsed = true;
		//从根节点configuration开始解析
		parseConfiguration(parser.evalNode("/configuration"));
		return configuration;
	}

	//从根节点开始解析配置
	private void parseConfiguration(XNode root) {
		try {
			//1.解析<properties>
			propertiesElement(root.evalNode("properties"));
			//2.解析<typeAliases>
			typeAliasesElement(root.evalNode("typeAliases"));
			//3.解析<plugins>
			pluginElement(root.evalNode("plugins"));
			//4.解析<objectFactory>
			objectFactoryElement(root.evalNode("objectFactory"));
			//5.解析<reflectorFactory>
			objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
			//6.解析<settings>
			settingsElement(root.evalNode("settings"));
			//7.解析<environments>
			environmentsElement(root.evalNode("environments"));
			//8.解析<databaseIdProvider>
			databaseIdProviderElement(root.evalNode("databaseIdProvider"));
			//9.解析<typeHandler>
			typeHandlerElement(root.evalNode("typeHandlers"));
			//10.解析<mappers>
			mapperElement(root.evalNode("mappers"));
		} catch (Exception e) {
			throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
		}
	}
	
	//1.解析<properties>
	private void propertiesElement(XNode context) throws Exception {
		if (context != null) {
			Properties defaults = context.getChildrenAsProperties();
			//获取resource属性值
			String resource = context.getStringAttribute("resource");
			//获取url属性值
			String url = context.getStringAttribute("url");
			if (resource != null && url != null) {
				throw new BuilderException("The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
			}
			if (resource != null) {
				//添加resource的全部配置
				defaults.putAll(Resources.getResourceAsProperties(resource));
			} else if (url != null) {
				//添加url的全部配置
				defaults.putAll(Resources.getUrlAsProperties(url));
			}
			Properties vars = configuration.getVariables();
			if (vars != null) {
				//添加构造方法传入的全部配置
				defaults.putAll(vars);
			}
			parser.setVariables(defaults);
			configuration.setVariables(defaults);
		}
	}

	//2.解析<typeAliases>节点
	private void typeAliasesElement(XNode parent) {
		if (parent != null) {
			for (XNode child : parent.getChildren()) {
				if ("package".equals(child.getName())) {
					// 如果是package
					String typeAliasPackage = child.getStringAttribute("name");
					// （一）调用TypeAliasRegistry.registerAliases，去包下找所有类,然后注册别名(有@Alias注解则用，没有则取类的simpleName)
					configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
				} else {
					// 如果是typeAlias
					String alias = child.getStringAttribute("alias");
					String type = child.getStringAttribute("type");
					try {
						Class<?> clazz = Resources.classForName(type);
						// 根据Class名字来注册类型别名
						// （二）调用TypeAliasRegistry.registerAlias
						if (alias == null) {
							// alias可以省略
							typeAliasRegistry.registerAlias(clazz);
						} else {
							typeAliasRegistry.registerAlias(alias, clazz);
						}
					} catch (ClassNotFoundException e) {
						throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
					}
				}
			}
		}
	}

	//3.解析<plugins>节点
	private void pluginElement(XNode parent) throws Exception {
		if (parent != null) {
			for (XNode child : parent.getChildren()) {
				String interceptor = child.getStringAttribute("interceptor");
				Properties properties = child.getChildrenAsProperties();
				Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).newInstance();
				interceptorInstance.setProperties(properties);
				// 调用InterceptorChain.addInterceptor
				configuration.addInterceptor(interceptorInstance);
			}
		}
	}

	//4.解析<objectFactory>节点
	private void objectFactoryElement(XNode context) throws Exception {
		if (context != null) {
			String type = context.getStringAttribute("type");
			Properties properties = context.getChildrenAsProperties();
			ObjectFactory factory = (ObjectFactory) resolveClass(type).newInstance();
			factory.setProperties(properties);
			configuration.setObjectFactory(factory);
		}
	}

	//5.解析<reflectorFactory>节点
	private void objectWrapperFactoryElement(XNode context) throws Exception {
		if (context != null) {
			String type = context.getStringAttribute("type");
			ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).newInstance();
			configuration.setObjectWrapperFactory(factory);
		}
	}

	//6.解析<settings>节点
	private void settingsElement(XNode context) throws Exception {
		if (context != null) {
			Properties props = context.getChildrenAsProperties();
			// 检查下是否在Configuration类里都有相应的setter方法（没有拼写错误）
			MetaClass metaConfig = MetaClass.forClass(Configuration.class);
			for (Object key : props.keySet()) {
				if (!metaConfig.hasSetter(String.valueOf(key))) {
					throw new BuilderException("The setting " + key
							+ " is not known.  Make sure you spelled it correctly (case sensitive).");
				}
			}
			//自动映射行为
			configuration.setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
			//是否启用缓存
			configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
			//设置代理工厂
			configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));
			//是否延迟加载
			configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
			//延迟加载时, 每种属性是否还要按需加载
			configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), true));
			//允不允许多种结果集从一个单独 的语句中返回
			configuration.setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));
			//使用列标签代替列名
			configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
			//允许 JDBC 支持生成的键
			configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
			//配置默认的执行器
			configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
			//超时时间
			configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
			//是否将下划线转成驼峰形式
			configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
			//嵌套语句上使用RowBounds
			configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));
			//默认用session级别的缓存
			configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
			//为null值设置jdbctype
			configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
			//Object的哪些方法将触发延迟加载
			configuration.setLazyLoadTriggerMethods(stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
			//使用安全的ResultHandler
			configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
			//动态SQL生成语言所使用的脚本语言
			configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
			//当结果集中含有Null值时是否执行映射对象的setter或者Map对象的put方法。此设置对于原始类型如int,boolean等无效。
			configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
			//logger名字的前缀
			configuration.setLogPrefix(props.getProperty("logPrefix"));
			//显式定义用什么log框架，不定义则用默认的自动发现jar包机制
			configuration.setLogImpl(resolveClass(props.getProperty("logImpl")));
			//配置工厂
			configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
		}
	}

	//7.解析<environments>节点
	private void environmentsElement(XNode context) throws Exception {
		if (context != null) {
			if (environment == null) {
				environment = context.getStringAttribute("default");
			}
			for (XNode child : context.getChildren()) {
				String id = child.getStringAttribute("id");
				// 循环比较id是否就是指定的environment
				if (isSpecifiedEnvironment(id)) {
					// 7.1事务管理器
					TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
					// 7.2数据源
					DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
					DataSource dataSource = dsFactory.getDataSource();
					Environment.Builder environmentBuilder = new Environment.Builder(id).transactionFactory(txFactory).dataSource(dataSource);
					configuration.setEnvironment(environmentBuilder.build());
				}
			}
		}
	}
	
	//7.1解析<transactionManager>节点, 事务管理器
	private TransactionFactory transactionManagerElement(XNode context) throws Exception {
		if (context != null) {
			String type = context.getStringAttribute("type");
			Properties props = context.getChildrenAsProperties();
			// 根据type="JDBC"解析返回适当的TransactionFactory
			TransactionFactory factory = (TransactionFactory) resolveClass(type).newInstance();
			factory.setProperties(props);
			return factory;
		}
		throw new BuilderException("Environment declaration requires a TransactionFactory.");
	}

	//7.2解析<dataSource>节点, 数据源
	private DataSourceFactory dataSourceElement(XNode context) throws Exception {
		if (context != null) {
			String type = context.getStringAttribute("type");
			Properties props = context.getChildrenAsProperties();
			// 根据type="POOLED"解析返回适当的DataSourceFactory
			DataSourceFactory factory = (DataSourceFactory) resolveClass(type).newInstance();
			factory.setProperties(props);
			return factory;
		}
		throw new BuilderException("Environment declaration requires a DataSourceFactory.");
	}
	
	//8.解析<databaseIdProvider>节点
	private void databaseIdProviderElement(XNode context) throws Exception {
		DatabaseIdProvider databaseIdProvider = null;
		if (context != null) {
			String type = context.getStringAttribute("type");
			// 与老版本兼容
			if ("VENDOR".equals(type)) {
				type = "DB_VENDOR";
			}
			Properties properties = context.getChildrenAsProperties();
			// "DB_VENDOR"-->VendorDatabaseIdProvider
			databaseIdProvider = (DatabaseIdProvider) resolveClass(type).newInstance();
			databaseIdProvider.setProperties(properties);
		}
		Environment environment = configuration.getEnvironment();
		if (environment != null && databaseIdProvider != null) {
			// 得到当前的databaseId，可以调用DatabaseMetaData.getDatabaseProductName()得到诸如"Oracle
			// (DataDirect)"的字符串，
			// 然后和预定义的property比较,得出目前究竟用的是什么数据库
			String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
			configuration.setDatabaseId(databaseId);
		}
	}

	//9.解析<typeHandlers>节点
	private void typeHandlerElement(XNode parent) throws Exception {
		if (parent != null) {
			for (XNode child : parent.getChildren()) {
				// 如果是package
				if ("package".equals(child.getName())) {
					String typeHandlerPackage = child.getStringAttribute("name");
					// （一）调用TypeHandlerRegistry.register，去包下找所有类
					typeHandlerRegistry.register(typeHandlerPackage);
				} else {
					// 如果是typeHandler
					String javaTypeName = child.getStringAttribute("javaType");
					String jdbcTypeName = child.getStringAttribute("jdbcType");
					String handlerTypeName = child.getStringAttribute("handler");
					Class<?> javaTypeClass = resolveClass(javaTypeName);
					JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
					Class<?> typeHandlerClass = resolveClass(handlerTypeName);
					// （二）调用TypeHandlerRegistry.register(以下是3种不同的参数形式)
					if (javaTypeClass != null) {
						if (jdbcType == null) {
							// javaType!=null && jdbcType==null
							typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
						} else {
							// javaType!=null && jdbcType!=null
							typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
						}
					} else {
						// 如果javaType为空, 执行下面操作
						typeHandlerRegistry.register(typeHandlerClass);
					}
				}
			}
		}
	}

	//10.解析<mappers>节点
	private void mapperElement(XNode parent) throws Exception {
		if (parent != null) {
			for (XNode child : parent.getChildren()) {
				if ("package".equals(child.getName())) {
					// 10.4自动扫描包下所有映射器
					String mapperPackage = child.getStringAttribute("name");
					configuration.addMappers(mapperPackage);
				} else {
					String resource = child.getStringAttribute("resource");
					String url = child.getStringAttribute("url");
					String mapperClass = child.getStringAttribute("class");
					if (resource != null && url == null && mapperClass == null) {
						// 10.1使用类路径
						ErrorContext.instance().resource(resource);
						InputStream inputStream = Resources.getResourceAsStream(resource);
						// 映射器比较复杂，调用XMLMapperBuilder
						// 注意在for循环里每个mapper都重新new一个XMLMapperBuilder，来解析
						XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource,
								configuration.getSqlFragments());
						mapperParser.parse();
					} else if (resource == null && url != null && mapperClass == null) {
						// 10.2使用绝对url路径
						ErrorContext.instance().resource(url);
						InputStream inputStream = Resources.getUrlAsStream(url);
						// 映射器比较复杂，调用XMLMapperBuilder
						XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url,
								configuration.getSqlFragments());
						mapperParser.parse();
					} else if (resource == null && url == null && mapperClass != null) {
						// 10.3使用java类名
						Class<?> mapperInterface = Resources.classForName(mapperClass);
						// 直接把这个映射加入配置
						configuration.addMapper(mapperInterface);
					} else {
						throw new BuilderException(
								"A mapper element may only specify a url, resource or class, but not more than one.");
					}
				}
			}
		}
	}

	// 比较id和environment是否相等
	private boolean isSpecifiedEnvironment(String id) {
		if (environment == null) {
			throw new BuilderException("No environment specified.");
		} else if (id == null) {
			throw new BuilderException("Environment requires an id attribute.");
		} else if (environment.equals(id)) {
			return true;
		}
		return false;
	}

}
