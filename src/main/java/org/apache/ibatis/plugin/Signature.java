package org.apache.ibatis.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//签名注解
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Signature {
	
	//被拦截的类
	Class<?> type();
	
	//被拦截的方法
	String method();

	//被拦截的参数
	Class<?>[] args();
	
}