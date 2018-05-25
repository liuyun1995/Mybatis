package org.apache.ibatis.plugin;

import java.util.Properties;

//拦截器
public interface Interceptor {

	//拦截方法
	Object intercept(Invocation invocation) throws Throwable;

	//插件方法
	Object plugin(Object target);

	//设置属性
	void setProperties(Properties properties);

}
