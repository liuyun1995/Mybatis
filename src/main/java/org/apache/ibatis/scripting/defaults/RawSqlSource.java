package org.apache.ibatis.scripting.defaults;

import java.util.HashMap;

import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.DynamicContext;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import org.apache.ibatis.session.Configuration;

/**
 * 原始SQL源码，比DynamicSqlSource快
 */
public class RawSqlSource implements SqlSource {

	private final SqlSource sqlSource;

	public RawSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType) {
		this(configuration, getSql(configuration, rootSqlNode), parameterType);
	}

	public RawSqlSource(Configuration configuration, String sql, Class<?> parameterType) {
		SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
		Class<?> clazz = parameterType == null ? Object.class : parameterType;
		sqlSource = sqlSourceParser.parse(sql, clazz, new HashMap<String, Object>());
	}

	private static String getSql(Configuration configuration, SqlNode rootSqlNode) {
		DynamicContext context = new DynamicContext(configuration, null);
		rootSqlNode.apply(context);
		return context.getSql();
	}

	public BoundSql getBoundSql(Object parameterObject) {
		return sqlSource.getBoundSql(parameterObject);
	}

}
