package org.apache.ibatis.builder.annotation;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;

public class ProviderSqlSource implements SqlSource {

	private SqlSourceBuilder sqlSourceParser;        //sqlSource构建器
	private Class<?> providerType;                   //provider类型
	private Method providerMethod;                   //provider方法
	private boolean providerTakesParameterObject;

	public ProviderSqlSource(Configuration config, Object provider) {
		String providerMethodName = null;
		try {
			this.sqlSourceParser = new SqlSourceBuilder(config);
			this.providerType = (Class<?>) provider.getClass().getMethod("type").invoke(provider);
			providerMethodName = (String) provider.getClass().getMethod("method").invoke(provider);

			for (Method m : this.providerType.getMethods()) {
				if (providerMethodName.equals(m.getName())) {
					if (m.getParameterTypes().length < 2 && m.getReturnType() == String.class) {
						this.providerMethod = m;
						this.providerTakesParameterObject = m.getParameterTypes().length == 1;
					}
				}
			}
		} catch (Exception e) {
			throw new BuilderException("Error creating SqlSource for SqlProvider.  Cause: " + e, e);
		}
		if (this.providerMethod == null) {
			throw new BuilderException("Error creating SqlSource for SqlProvider. Method '" + providerMethodName
					+ "' not found in SqlProvider '" + this.providerType.getName() + "'.");
		}
	}

	//获取绑定的sql
	public BoundSql getBoundSql(Object parameterObject) {
		//创建SqlSource对象
		SqlSource sqlSource = createSqlSource(parameterObject);
		//传入参数对象, 获取绑定的sql
		return sqlSource.getBoundSql(parameterObject);
	}

	//创建SqlSource
	private SqlSource createSqlSource(Object parameterObject) {
		try {
			String sql;
			if (providerTakesParameterObject) {
				sql = (String) providerMethod.invoke(providerType.newInstance(), parameterObject);
			} else {
				sql = (String) providerMethod.invoke(providerType.newInstance());
			}
			Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
			return sqlSourceParser.parse(sql, parameterType, new HashMap<String, Object>());
		} catch (Exception e) {
			throw new BuilderException("Error invoking SqlProvider method (" + providerType.getName() + "."
					+ providerMethod.getName() + ").  Cause: " + e, e);
		}
	}

}
