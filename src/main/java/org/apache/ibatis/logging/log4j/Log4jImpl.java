package org.apache.ibatis.logging.log4j;

import org.apache.ibatis.logging.Log;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Log4jImpl implements Log {

	private static final String FQCN = Log4jImpl.class.getName();

	private Logger log;

	public Log4jImpl(String clazz) {
		log = Logger.getLogger(clazz);
	}

	public boolean isDebugEnabled() {
		return log.isDebugEnabled();
	}

	public boolean isTraceEnabled() {
		return log.isTraceEnabled();
	}

	public void error(String s, Throwable e) {
		log.log(FQCN, Level.ERROR, s, e);
	}

	public void error(String s) {
		log.log(FQCN, Level.ERROR, s, null);
	}

	public void debug(String s) {
		log.log(FQCN, Level.DEBUG, s, null);
	}

	public void trace(String s) {
		log.log(FQCN, Level.TRACE, s, null);
	}

	public void warn(String s) {
		log.log(FQCN, Level.WARN, s, null);
	}

}
