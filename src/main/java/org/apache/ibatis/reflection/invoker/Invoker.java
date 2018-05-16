package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.InvocationTargetException;

//方法调用接口
public interface Invoker {
	
	//调用方法
	Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException;

	//获取类型
	Class<?> getType();
	
}
