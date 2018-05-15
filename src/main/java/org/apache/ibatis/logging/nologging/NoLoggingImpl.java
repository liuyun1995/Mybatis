package org.apache.ibatis.logging.nologging;

import org.apache.ibatis.logging.Log;

public class NoLoggingImpl implements Log {

	public NoLoggingImpl(String clazz) {
		// Do Nothing
	}

	public boolean isDebugEnabled() {
		return false;
	}

	public boolean isTraceEnabled() {
		return false;
	}

	public void error(String s, Throwable e) {
		// Do Nothing
	}

	public void error(String s) {
		// Do Nothing
	}

	public void debug(String s) {
		// Do Nothing
	}

	public void trace(String s) {
		// Do Nothing
	}

	public void warn(String s) {
		// Do Nothing
	}

}
