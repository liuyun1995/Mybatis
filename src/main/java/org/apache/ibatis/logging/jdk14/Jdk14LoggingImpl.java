package org.apache.ibatis.logging.jdk14;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.ibatis.logging.Log;

public class Jdk14LoggingImpl implements Log {

	private Logger log;

	public Jdk14LoggingImpl(String clazz) {
		log = Logger.getLogger(clazz);
	}

	public boolean isDebugEnabled() {
		return log.isLoggable(Level.FINE);
	}

	public boolean isTraceEnabled() {
		return log.isLoggable(Level.FINER);
	}

	public void error(String s, Throwable e) {
		log.log(Level.SEVERE, s, e);
	}

	public void error(String s) {
		log.log(Level.SEVERE, s);
	}

	public void debug(String s) {
		log.log(Level.FINE, s);
	}

	public void trace(String s) {
		log.log(Level.FINER, s);
	}

	public void warn(String s) {
		log.log(Level.WARNING, s);
	}

}
