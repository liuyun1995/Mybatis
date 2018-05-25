package org.apache.ibatis.plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.reflection.ExceptionUtil;

//插件,用的代理模式
public class Plugin implements InvocationHandler {

	private Object target;                             //目标对象
	private Interceptor interceptor;                   //拦截器
	private Map<Class<?>, Set<Method>> signatureMap;   //签名映射

	//构造器
	private Plugin(Object target, Interceptor interceptor, Map<Class<?>, Set<Method>> signatureMap) {
		this.target = target;
		this.interceptor = interceptor;
		this.signatureMap = signatureMap;
	}

	//包装方法
	public static Object wrap(Object target, Interceptor interceptor) {
		//获取签名映射
		Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);
		//获取目标类型
		Class<?> type = target.getClass();
		//获取所有接口
		Class<?>[] interfaces = getAllInterfaces(type, signatureMap);
		//若接口数大于0, 则返回代理对象
		if (interfaces.length > 0) {
			return Proxy.newProxyInstance(type.getClassLoader(), interfaces,
					new Plugin(target, interceptor, signatureMap));
		}
		//否则返回目标对象
		return target;
	}

	//调用方法
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			//获取该类型所有要拦截的方法
			Set<Method> methods = signatureMap.get(method.getDeclaringClass());
			//若该方法在要拦截的方法集合中
			if (methods != null && methods.contains(method)) {
				//调用拦截器进行拦截
				return interceptor.intercept(new Invocation(target, method, args));
			}
			//拦截后再执行原来的方法
			return method.invoke(target, args);
		} catch (Exception e) {
			throw ExceptionUtil.unwrapThrowable(e);
		}
	}

	//获取签名映射
	private static Map<Class<?>, Set<Method>> getSignatureMap(Interceptor interceptor) {
		//获取拦截器上的Intercepts注解
		Intercepts interceptsAnnotation = interceptor.getClass().getAnnotation(Intercepts.class);
		//若没有Intercepts注解则报错
		if (interceptsAnnotation == null) {
			throw new PluginException("No @Intercepts annotation was found in interceptor " + interceptor.getClass().getName());
		}
		//获取Intercepts注解的value值
		Signature[] sigs = interceptsAnnotation.value();
		Map<Class<?>, Set<Method>> signatureMap = new HashMap<Class<?>, Set<Method>>();
		//遍历Signature注解数组
		for (Signature sig : sigs) {
			//根据Signature注解的type值获取方法集合
			Set<Method> methods = signatureMap.get(sig.type());
			if (methods == null) {
				methods = new HashSet<Method>();
				signatureMap.put(sig.type(), methods);
			}
			try {
				//获取要拦截类型的特定方法
				Method method = sig.type().getMethod(sig.method(), sig.args());
				//添加进方法集合中
				methods.add(method);
			} catch (NoSuchMethodException e) {
				throw new PluginException(
						"Could not find method on " + sig.type() + " named " + sig.method() + ". Cause: " + e, e);
			}
		}
		return signatureMap;
	}

	//获取所有接口
	private static Class<?>[] getAllInterfaces(Class<?> type, Map<Class<?>, Set<Method>> signatureMap) {
		Set<Class<?>> interfaces = new HashSet<Class<?>>();
		while (type != null) {
			//遍历该类型的所有接口
			for (Class<?> c : type.getInterfaces()) {
				if (signatureMap.containsKey(c)) {
					interfaces.add(c);
				}
			}
			//查看该类型的父类
			type = type.getSuperclass();
		}
		return interfaces.toArray(new Class<?>[interfaces.size()]);
	}

}
