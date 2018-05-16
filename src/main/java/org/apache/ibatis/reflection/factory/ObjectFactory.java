package org.apache.ibatis.reflection.factory;

import java.util.List;
import java.util.Properties;

//对象工厂接口
public interface ObjectFactory {
	
	void setProperties(Properties properties);
	
	<T> T create(Class<T> type);

	<T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);

	<T> boolean isCollection(Class<T> type);

}
