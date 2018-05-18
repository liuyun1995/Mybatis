package org.apache.ibatis.binding;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.SqlSession;

//映射器代理
public class MapperProxy<T> implements InvocationHandler, Serializable {

	private static final long serialVersionUID = -6424540398559729838L;
	
	private final SqlSession sqlSession;                    //SqlSession
	private final Class<T> mapperInterface;                 //mapper接口
	private final Map<Method, MapperMethod> methodCache;    //方法缓存映射

	public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
		this.sqlSession = sqlSession;
		this.mapperInterface = mapperInterface;
		this.methodCache = methodCache;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		//代理以后，所有Mapper的方法调用时，都会调用这个invoke方法
		//并不是任何一个方法都需要执行调用代理对象进行执行，如果这个方法是Object中通用的方法（toString、hashCode等）无需执行
		
		//如果是Object上的方法就直接调用
		if (Object.class.equals(method.getDeclaringClass())) {
			try {
				return method.invoke(this, args);
			} catch (Throwable t) {
				throw ExceptionUtil.unwrapThrowable(t);
			}
		}
		//去缓存中找MapperMethod
		final MapperMethod mapperMethod = cachedMapperMethod(method);
		//执行方法
		return mapperMethod.execute(sqlSession, args);
	}

	//去缓存中找MapperMethod
	private MapperMethod cachedMapperMethod(Method method) {
		MapperMethod mapperMethod = methodCache.get(method);
		if (mapperMethod == null) {
			mapperMethod = new MapperMethod(mapperInterface, method, sqlSession.getConfiguration());
			methodCache.put(method, mapperMethod);
		}
		return mapperMethod;
	}

}
