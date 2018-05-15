package org.apache.ibatis.exceptions;

/**
 * 持久化异常 可以看到这个类只是继承了一个废弃的IbatisException，其他都一样
 */
@SuppressWarnings("deprecation")
public class PersistenceException extends IbatisException {

	private static final long serialVersionUID = -7537395265357977271L;

	public PersistenceException() {
		super();
	}

	public PersistenceException(String message) {
		super(message);
	}

	public PersistenceException(String message, Throwable cause) {
		super(message, cause);
	}

	public PersistenceException(Throwable cause) {
		super(cause);
	}
}
