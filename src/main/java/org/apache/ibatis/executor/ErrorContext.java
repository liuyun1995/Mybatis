package org.apache.ibatis.executor;

//错误上下文
public class ErrorContext {
	
	private static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");
	private static final ThreadLocal<ErrorContext> LOCAL = new ThreadLocal<ErrorContext>();

	private ErrorContext stored;
	private String resource;
	private String activity;
	private String object;
	private String message;
	private String sql;
	private Throwable cause;
	
	private ErrorContext() {}

	//获取错误上下文实例
	public static ErrorContext instance() {
		ErrorContext context = LOCAL.get();
		if (context == null) {
			context = new ErrorContext();
			LOCAL.set(context);
		}
		return context;
	}

	//存放错误上下文
	public ErrorContext store() {
		stored = this;
		LOCAL.set(new ErrorContext());
		return LOCAL.get();
	}

	//召回错误上下文
	public ErrorContext recall() {
		if (stored != null) {
			LOCAL.set(stored);
			stored = null;
		}
		return LOCAL.get();
	}

	//设置resource
	public ErrorContext resource(String resource) {
		this.resource = resource;
		return this;
	}

	//设置activity
	public ErrorContext activity(String activity) {
		this.activity = activity;
		return this;
	}

	//设置object
	public ErrorContext object(String object) {
		this.object = object;
		return this;
	}

	//设置message
	public ErrorContext message(String message) {
		this.message = message;
		return this;
	}

	//设置sql
	public ErrorContext sql(String sql) {
		this.sql = sql;
		return this;
	}

	//设置cause
	public ErrorContext cause(Throwable cause) {
		this.cause = cause;
		return this;
	}

	//清空重置
	public ErrorContext reset() {
		resource = null;
		activity = null;
		object = null;
		message = null;
		sql = null;
		cause = null;
		LOCAL.remove();
		return this;
	}
	
	@Override
	public String toString() {
		StringBuilder description = new StringBuilder();
		if (this.message != null) {
			description.append(LINE_SEPARATOR);
			description.append("### ");
			description.append(this.message);
		}
		if (resource != null) {
			description.append(LINE_SEPARATOR);
			description.append("### The error may exist in ");
			description.append(resource);
		}
		if (object != null) {
			description.append(LINE_SEPARATOR);
			description.append("### The error may involve ");
			description.append(object);
		}
		if (activity != null) {
			description.append(LINE_SEPARATOR);
			description.append("### The error occurred while ");
			description.append(activity);
		}
		if (sql != null) {
			description.append(LINE_SEPARATOR);
			description.append("### SQL: ");
			description.append(sql.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').trim());
		}
		if (cause != null) {
			description.append(LINE_SEPARATOR);
			description.append("### Cause: ");
			description.append(cause.toString());
		}
		return description.toString();
	}

}
