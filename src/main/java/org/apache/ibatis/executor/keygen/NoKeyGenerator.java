package org.apache.ibatis.executor.keygen;

import java.sql.Statement;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;

/**
 * 不用键值生成器, MappedStatement有一个keyGenerator属性，默认的就用NoKeyGenerator
 */
public class NoKeyGenerator implements KeyGenerator {

	// 都是空方法
	public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
		// Do Nothing
	}

	public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
		// Do Nothing
	}

}
