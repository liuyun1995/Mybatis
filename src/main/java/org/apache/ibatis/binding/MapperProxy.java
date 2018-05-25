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
	
	private final SqlSession sqlSession;                    //sqlSession
	private final Class<T> mapperInterface;                 //mapper接口
	private final Map<Method, MapperMethod> methodCache;    //方法缓存映射

	public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
		this.sqlSession = sqlSession;
		this.mapperInterface = mapperInterface;
		this.methodCache = methodCache;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		//如果是Object上的方法就调用原方法
		if (Object.class.equals(method.getDeclaringClass())) {
			try {
				return method.invoke(this, args);
			} catch (Throwable t) {
				throw ExceptionUtil.unwrapThrowable(t);
			}
		}
		//否则,根据原方法在缓存中找到MapperMethod
		final MapperMethod mapperMethod = cachedMapperMethod(method);
		//调用MapperMethod的执行方法
		return mapperMethod.execute(sqlSession, args);
	}

	//在缓存中获取MapperMethod
	private MapperMethod cachedMapperMethod(Method method) {
		//根据原方法在缓存中找到MapperMethod
		MapperMethod mapperMethod = methodCache.get(method);
		//如果mapperMethod为空, 就新建一个MapperMethod对象, 并放入缓存中
		if (mapperMethod == null) {
			mapperMethod = new MapperMethod(mapperInterface, method, sqlSession.getConfiguration());
			methodCache.put(method, mapperMethod);
		}
		return mapperMethod;
	}

}
