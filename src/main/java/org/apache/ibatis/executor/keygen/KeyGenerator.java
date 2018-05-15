package org.apache.ibatis.executor.keygen;

import java.sql.Statement;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;

/**
 * 键值生成器
 */
public interface KeyGenerator {

	// 定了2个回调方法，processBefore,processAfter
	void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter);

	void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter);

}
