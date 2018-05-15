package org.apache.ibatis.scripting.xmltags;

import java.util.Map;

import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;

//动态SQL源码
public class DynamicSqlSource implements SqlSource {

	private Configuration configuration;
	private SqlNode rootSqlNode;

	public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
		this.configuration = configuration;
		this.rootSqlNode = rootSqlNode;
	}

	// 得到绑定的SQL
	public BoundSql getBoundSql(Object parameterObject) {
		// 生成一个动态上下文
		DynamicContext context = new DynamicContext(configuration, parameterObject);
		// 这里SqlNode.apply只是将${}这种参数替换掉，并没有替换#{}这种参数
		rootSqlNode.apply(context);
		// 调用SqlSourceBuilder
		SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
		Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
		// SqlSourceBuilder.parse,注意这里返回的是StaticSqlSource,解析完了就把那些参数都替换成?了，也就是最基本的JDBC的SQL写法
		SqlSource sqlSource = sqlSourceParser.parse(context.getSql(), parameterType, context.getBindings());
		// 看似是又去递归调用SqlSource.getBoundSql，其实因为是StaticSqlSource，所以没问题，不是递归调用
		BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
		for (Map.Entry<String, Object> entry : context.getBindings().entrySet()) {
			boundSql.setAdditionalParameter(entry.getKey(), entry.getValue());
		}
		return boundSql;
	}

}
