package org.apache.ibatis.reflection.factory;

import java.util.List;
import java.util.Properties;

/**
 * 对象工厂，所有对象都要由工厂来产生
 */
public interface ObjectFactory {
	
	void setProperties(Properties properties);
	
	<T> T create(Class<T> type);

	/**
	 * 生产对象，使用指定的构造函数和构造函数参数
	 */
	<T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);

	/**
	 * collections. 返回这个对象是否是集合，为了支持Scala collections？
	 */
	<T> boolean isCollection(Class<T> type);

}
