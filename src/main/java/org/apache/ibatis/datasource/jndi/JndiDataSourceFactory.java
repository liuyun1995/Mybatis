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
public class JndiDataSourceFactory implements DataSourceFactory {

	public static final String INITIAL_CONTEXT = "initial_context";
	public static final String DATA_SOURCE = "data_source";
	public static final String ENV_PREFIX = "env.";
	private DataSource dataSource;

	//设置属性
	public void setProperties(Properties properties) {
		try {
			InitialContext initCtx = null;
			//获取环境属性
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
		//遍历所有的配置
		for (Entry<Object, Object> entry : allProps.entrySet()) {
			//获取配置的键
			String key = (String) entry.getKey();
			//获取配置的值
			String value = (String) entry.getValue();
			//如果键是以"env."开头的
			if (key.startsWith(PREFIX)) {
				if (contextProperties == null) {
					contextProperties = new Properties();
				}
				//截取前缀后再放入
				contextProperties.put(key.substring(PREFIX.length()), value);
			}
		}
		return contextProperties;
	}

}
