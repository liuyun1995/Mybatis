package org.apache.ibatis.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

//调用
public class Invocation {

	private Object target;   //目标对象
	private Method method;   //调用方法
	private Object[] args;   //方法参数

	//构造器
	public Invocation(Object target, Method method, Object[] args) {
		this.target = target;
		this.method = method;
		this.args = args;
	}

	//获取目标对象
	public Object getTarget() {
		return target;
	}

	//获取调用方法
	public Method getMethod() {
		return method;
	}

	//获取方法参数
	public Object[] getArgs() {
		return args;
	}

	//继续执行
	public Object proceed() throws InvocationTargetException, IllegalAccessException {
		return method.invoke(target, args);
	}

}
