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
			//遍历所有子结点
			for (XNode child : parent.getChildren()) {
				//如果是<package>标签
				if ("package".equals(child.getName())) {
					//获取name属性值
					String typeAliasPackage = child.getStringAttribute("name");
					//获取别名注册工厂并注册别名
					configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
				} else {
					//获取alias属性值
					String alias = child.getStringAttribute("alias");
					//获取type属性值
					String type = child.getStringAttribute("type");
					try {
						//根据type值来获取类型
						Class<?> clazz = Resources.classForName(type);
						//如果别名为空则根据类型注册
						if (alias == null) {
							typeAliasRegistry.registerAlias(clazz);
						//否则根据别名和类型注册
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
				//获取interceptor属性值
				String interceptor = child.getStringAttribute("interceptor");
				Properties properties = child.getChildrenAsProperties();
				//获取拦截器实例
				Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).newInstance();
				//设置拦截器的属性
				interceptorInstance.setProperties(properties);
				//在配置信息中添加拦截器
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
			//获取结点下的所有属性
			Properties props = context.getChildrenAsProperties();
			//获取Configuration的原类型
			MetaClass metaConfig = MetaClass.forClass(Configuration.class);
			for (Object key : props.keySet()) {
				//如果key没有setter方法, 则报错
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
			//设置执行器类型, 默认为SIMPLE
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
				//获取id属性值
				String id = child.getStringAttribute("id");
				//比较id是否是指定的environment
				if (isSpecifiedEnvironment(id)) {
					//获取事务管理器工厂
					TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
					//获取数据源工厂
					DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
					//获取数据源
					DataSource dataSource = dsFactory.getDataSource();
					//新建Environment对象
					Environment.Builder environmentBuilder = new Environment.Builder(id).transactionFactory(txFactory).dataSource(dataSource);
					configuration.setEnvironment(environmentBuilder.build());
				}
			}
		}
	}
	
	//7.1解析<transactionManager>节点
	private TransactionFactory transactionManagerElement(XNode context) throws Exception {
		if (context != null) {
			//获取type属性值
			String type = context.getStringAttribute("type");
			//将所有子结点转为属性
			Properties props = context.getChildrenAsProperties();
			//根据type值获取事务工厂实例
			TransactionFactory factory = (TransactionFactory) resolveClass(type).newInstance();
			//为事务工厂设置属性
			factory.setProperties(props);
			return factory;
		}
		throw new BuilderException("Environment declaration requires a TransactionFactory.");
	}

	//7.2解析<dataSource>节点
	private DataSourceFactory dataSourceElement(XNode context) throws Exception {
		if (context != null) {
			//获取type属性值
			String type = context.getStringAttribute("type");
			//将所有子结点转为属性
			Properties props = context.getChildrenAsProperties();
			//根据type值获取数据源工厂实例
			DataSourceFactory factory = (DataSourceFactory) resolveClass(type).newInstance();
			//为数据源工厂设置属性
			factory.setProperties(props);
			return factory;
		}
		throw new BuilderException("Environment declaration requires a DataSourceFactory.");
	}
	
	//8.解析<databaseIdProvider>节点
	private void databaseIdProviderElement(XNode context) throws Exception {
		DatabaseIdProvider databaseIdProvider = null;
		if (context != null) {
			//获取type属性值
			String type = context.getStringAttribute("type");
			//与老版本兼容
			if ("VENDOR".equals(type)) type = "DB_VENDOR";
			//将所有子结点转为属性
			Properties properties = context.getChildrenAsProperties();
			//根据type值获取数据库ID提供者实例
			databaseIdProvider = (DatabaseIdProvider) resolveClass(type).newInstance();
			//为数据库ID提供者设置属性
			databaseIdProvider.setProperties(properties);
		}
		//获取环境信息
		Environment environment = configuration.getEnvironment();
		if (environment != null && databaseIdProvider != null) {
			//根据数据源获取数据库ID
			String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
			//在配置信息中设置数据库ID
			configuration.setDatabaseId(databaseId);
		}
	}

	//9.解析<typeHandlers>节点
	private void typeHandlerElement(XNode parent) throws Exception {
		if (parent != null) {
			for (XNode child : parent.getChildren()) {
				//如果是<package>标签
				if ("package".equals(child.getName())) {
					//获取name属性值
					String typeHandlerPackage = child.getStringAttribute("name");
					//根据包名注册所有类型处理器
					typeHandlerRegistry.register(typeHandlerPackage);
				} else {
					//获取javaType属性值
					String javaTypeName = child.getStringAttribute("javaType");
					//获取jdbcType属性值
					String jdbcTypeName = child.getStringAttribute("jdbcType");
					//获取handler属性值
					String handlerTypeName = child.getStringAttribute("handler");
					//获取java类型
					Class<?> javaTypeClass = resolveClass(javaTypeName);
					//获取JdbcType实例
					JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
					//获取类型处理器类型
					Class<?> typeHandlerClass = resolveClass(handlerTypeName);
					if (javaTypeClass != null) {
						if (jdbcType == null) {
							//如果javaType不空, jdbcType为空
							typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
						} else {
							//如果javaType不空, jdbcType不空
							typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
						}
					} else {
						//如果javaType为空, 则只使用类型处理器类注册
						typeHandlerRegistry.register(typeHandlerClass);
					}
				}
			}
		}
	}

	//10.解析<mappers>节点
	private void mapperElement(XNode parent) throws Exception {
		if (parent != null) {
			//遍历所有子节点
			for (XNode child : parent.getChildren()) {
				//解析<package>节点
				if ("package".equals(child.getName())) {
					String mapperPackage = child.getStringAttribute("name");
					//添加指定包下的所有Mapper
					configuration.addMappers(mapperPackage);
				//解析<mapper>节点
				} else {
					//获取resource属性
					String resource = child.getStringAttribute("resource");
					//获取url属性
					String url = child.getStringAttribute("url");
					//获取class属性
					String mapperClass = child.getStringAttribute("class");
					
					//使用resource相对路径
					if (resource != null && url == null && mapperClass == null) {
						ErrorContext.instance().resource(resource);
						InputStream inputStream = Resources.getResourceAsStream(resource);
						//新建XMLMapperBuilder来解析
						XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
						mapperParser.parse();
					//使用url绝对路径
					} else if (resource == null && url != null && mapperClass == null) {
						ErrorContext.instance().resource(url);
						InputStream inputStream = Resources.getUrlAsStream(url);
						//新建XMLMapperBuilder来解析
						XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
						mapperParser.parse();
					//使用mapperClass类路径
					} else if (resource == null && url == null && mapperClass != null) {
						//根据类路径获取Class
						Class<?> mapperInterface = Resources.classForName(mapperClass);
						//直接将Class加入配置中
						configuration.addMapper(mapperInterface);
					} else {
						throw new BuilderException(
								"A mapper element may only specify a url, resource or class, but not more than one.");
					}
				}
			}
		}
	}

	//比较id和environment是否相等
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
