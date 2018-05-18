package org.apache.ibatis.binding;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.session.SqlSession;

//映射器代理工厂
public class MapperProxyFactory<T> {

	//Mapper接口类型
	private final Class<T> mapperInterface;
	//方法缓存映射
	private Map<Method, MapperMethod> methodCache = new ConcurrentHashMap<Method, MapperMethod>();

	//构造器(传入Mapper类型)
	public MapperProxyFactory(Class<T> mapperInterface) {
		this.mapperInterface = mapperInterface;
	}

	//获取Mapper类型
	public Class<T> getMapperInterface() {
		return mapperInterface;
	}

	//获取方法缓存映射
	public Map<Method, MapperMethod> getMethodCache() {
		return methodCache;
	}

	//实例化一个Mapper代理
	@SuppressWarnings("unchecked")
	protected T newInstance(MapperProxy<T> mapperProxy) {
		//使用JDK动态代理
		return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface },
				mapperProxy);
	}

	//实例化一个Mapper代理
	public T newInstance(SqlSession sqlSession) {
		final MapperProxy<T> mapperProxy = new MapperProxy<T>(sqlSession, mapperInterface, methodCache);
		return newInstance(mapperProxy);
	}

}
