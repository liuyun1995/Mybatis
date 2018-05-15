package org.apache.ibatis.datasource.jndi;

import java.util.Map.Entry;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.ibatis.datasource.DataSourceException;
import org.apache.ibatis.datasource.DataSourceFactory;

//JNDI数据源工厂
//这个数据源的实现是为了使用如 Spring或应用服务器这类的容器, 容器可以集中或在外部配置数据源,然后放置一个JNDI上下文的引用
public class JndiDataSourceFactory implements DataSourceFactory {

	public static final String INITIAL_CONTEXT = "initial_context";
	public static final String DATA_SOURCE = "data_source";
	// 和其他数据源配置相似, 它也可以通过名为 “env.” 的前缀直接向初始上下文发送属性。 比如:
	// env.encoding=UTF8
	public static final String ENV_PREFIX = "env.";

	private DataSource dataSource;

	public void setProperties(Properties properties) {
		try {
			InitialContext initCtx = null;
			Properties env = getEnvProperties(properties);
			if (env == null) {
				initCtx = new InitialContext();
			} else {
				initCtx = new InitialContext(env);
			}
			if (properties.containsKey(INITIAL_CONTEXT) && properties.containsKey(DATA_SOURCE)) {
				Context ctx = (Context) initCtx.lookup(properties.getProperty(INITIAL_CONTEXT));
				dataSource = (DataSource) ctx.lookup(properties.getProperty(DATA_SOURCE));
			} else if (properties.containsKey(DATA_SOURCE)) {
				dataSource = (DataSource) initCtx.lookup(properties.getProperty(DATA_SOURCE));
			}
		} catch (NamingException e) {
			throw new DataSourceException("There was an error configuring JndiDataSourceTransactionPool. Cause: " + e, e);
		}
	}

	//获取数据源
	public DataSource getDataSource() {
		return dataSource;
	}

	//获取环境配置
	private static Properties getEnvProperties(Properties allProps) {
		final String PREFIX = ENV_PREFIX;
		Properties contextProperties = null;
		for (Entry<Object, Object> entry : allProps.entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			// 和其他数据源配置相似, 它也可以通过名为 “env.” 的前缀直接向初始上下文发送属性。 比如:
			// env.encoding=UTF8
			if (key.startsWith(PREFIX)) {
				if (contextProperties == null) {
					contextProperties = new Properties();
				}
				contextProperties.put(key.substring(PREFIX.length()), value);
			}
		}
		return contextProperties;
	}

}
