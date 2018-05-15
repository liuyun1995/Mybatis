package org.apache.ibatis.logging.stdout;

import org.apache.ibatis.logging.Log;

/**
 * 打印到控制台的Log
 */
public class StdOutImpl implements Log {

	public StdOutImpl(String clazz) {
		// Do Nothing
	}

	public boolean isDebugEnabled() {
		return true;
	}

	public boolean isTraceEnabled() {
		return true;
	}

	public void error(String s, Throwable e) {
		System.err.println(s);
		e.printStackTrace(System.err);
	}

	public void error(String s) {
		System.err.println(s);
	}

	public void debug(String s) {
		System.out.println(s);
	}

	public void trace(String s) {
		System.out.println(s);
	}

	public void warn(String s) {
		System.out.println(s);
	}
}
