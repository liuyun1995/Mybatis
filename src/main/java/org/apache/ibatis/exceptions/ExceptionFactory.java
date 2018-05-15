package org.apache.ibatis.exceptions;

import org.apache.ibatis.executor.ErrorContext;

//异常工厂
public class ExceptionFactory {

	private ExceptionFactory() {}

	//异常包装方法
	public static RuntimeException wrapException(String message, Exception e) {
		return new PersistenceException(ErrorContext.instance().message(message).cause(e).toString(), e);
	}

}
