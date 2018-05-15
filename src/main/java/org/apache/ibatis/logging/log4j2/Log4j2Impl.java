package org.apache.ibatis.logging.log4j2;

import org.apache.ibatis.logging.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.AbstractLogger;

public class Log4j2Impl implements Log {

	private Log log;

	public Log4j2Impl(String clazz) {
		Logger logger = LogManager.getLogger(clazz);

		if (logger instanceof AbstractLogger) {
			log = new Log4j2AbstractLoggerImpl((AbstractLogger) logger);
		} else {
			log = new Log4j2LoggerImpl(logger);
		}
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
