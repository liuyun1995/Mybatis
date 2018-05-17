package org.apache.ibatis.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//签名注解
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Signature {
	// 就是定义哪些类，方法，参数需要被拦截
	Class<?> type();

	String method();

	Class<?>[] args();
}