package org.apache.ibatis.scripting;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;

//脚本语言驱动
public interface LanguageDriver {
	
	//创建参数处理器
	ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql);

	//创建SQL源码(xml方式)
	SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType);
	
	//创建SQL源码(注解方式)
	SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType);

}
