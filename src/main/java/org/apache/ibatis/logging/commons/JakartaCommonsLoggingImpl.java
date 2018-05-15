package org.apache.ibatis.logging.commons;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 用的commons logging里的Log和LogFactory
 */
public class JakartaCommonsLoggingImpl implements org.apache.ibatis.logging.Log {

	private Log log;

	public JakartaCommonsLoggingImpl(String clazz) {
		log = LogFactory.getLog(clazz);
	}

	public boolean isDebugEnabled() {
		return log.isDebugEnabled();
	}

	public boolean isTraceEnabled() {
		return log.isTraceEnabled();
	}

	public void error(String s, Throwable e) {
		log.error(s, e);
	}

	public void error(String s) {
		log.error(s);
	}

	public void debug(String s) {
		log.debug(s);
	}

	public void trace(String s) {
		log.trace(s);
	}

	public void warn(String s) {
		log.warn(s);
	}

}
