package org.apache.ibatis.logging.slf4j;

import org.apache.ibatis.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;

/**
 * slf4j的logger
 */
public class Slf4jImpl implements Log {

	// 代理模式，委派给Slf4jLoggerImpl或者Slf4jLocationAwareLoggerImpl,所以这个类只是一个wrapper
	private Log log;

	public Slf4jImpl(String clazz) {
		Logger logger = LoggerFactory.getLogger(clazz);

		if (logger instanceof LocationAwareLogger) {
			try {
				// check for slf4j >= 1.6 method signature
				logger.getClass().getMethod("log", Marker.class, String.class, int.class, String.class, Object[].class,
						Throwable.class);
				log = new Slf4jLocationAwareLoggerImpl((LocationAwareLogger) logger);
				return;
			} catch (SecurityException e) {
				// fail-back to Slf4jLoggerImpl
			} catch (NoSuchMethodException e) {
				// fail-back to Slf4jLoggerImpl
			}
		}

		// Logger is not LocationAwareLogger or slf4j version < 1.6
		log = new Slf4jLoggerImpl(logger);
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
