package org.apache.ibatis.builder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

//基础构建器
public abstract class BaseBuilder {

	protected final Configuration configuration;             //配置代表类
	protected final TypeAliasRegistry typeAliasRegistry;     //类型别名注册器
	protected final TypeHandlerRegistry typeHandlerRegistry; //类型处理器注册器

	public BaseBuilder(Configuration configuration) {
		this.configuration = configuration;
		this.typeAliasRegistry = this.configuration.getTypeAliasRegistry();
		this.typeHandlerRegistry = this.configuration.getTypeHandlerRegistry();
	}

	//获取配置信息
	public Configuration getConfiguration() {
		return configuration;
	}

	//解析表达式
	protected Pattern parseExpression(String regex, String defaultValue) {
		return Pattern.compile(regex == null ? defaultValue : regex);
	}

	//转成Boolean类型
	protected Boolean booleanValueOf(String value, Boolean defaultValue) {
		return value == null ? defaultValue : Boolean.valueOf(value);
	}

	//转成Integer类型
	protected Integer integerValueOf(String value, Integer defaultValue) {
		return value == null ? defaultValue : Integer.valueOf(value);
	}
	
	//转成Set类型
	protected Set<String> stringSetValueOf(String value, String defaultValue) {
		value = (value == null ? defaultValue : value);
		return new HashSet<String>(Arrays.asList(value.split(",")));
	}

	//解析JdbcType
	protected JdbcType resolveJdbcType(String alias) {
		if (alias == null) {
			return null;
		}
		try {
			return JdbcType.valueOf(alias);
		} catch (IllegalArgumentException e) {
			throw new BuilderException("Error resolving JdbcType. Cause: " + e, e);
		}
	}

	//解析ResultSetType
	protected ResultSetType resolveResultSetType(String alias) {
		if (alias == null) {
			return null;
		}
		try {
			return ResultSetType.valueOf(alias);
		} catch (IllegalArgumentException e) {
			throw new BuilderException("Error resolving ResultSetType. Cause: " + e, e);
		}
	}

	//解析ParameterMode
	protected ParameterMode resolveParameterMode(String alias) {
		if (alias == null) {
			return null;
		}
		try {
			return ParameterMode.valueOf(alias);
		} catch (IllegalArgumentException e) {
			throw new BuilderException("Error resolving ParameterMode. Cause: " + e, e);
		}
	}

	//根据别名创建实例
	protected Object createInstance(String alias) {
		Class<?> clazz = resolveClass(alias);
		if (clazz == null) {
			return null;
		}
		try {
			return resolveClass(alias).newInstance();
		} catch (Exception e) {
			throw new BuilderException("Error creating instance. Cause: " + e, e);
		}
	}

	//根据别名解析Class
	protected Class<?> resolveClass(String alias) {
		if (alias == null) {
			return null;
		}
		try {
			return resolveAlias(alias);
		} catch (Exception e) {
			throw new BuilderException("Error resolving class. Cause: " + e, e);
		}
	}

	//解析类型处理器(根据java类型和类型处理器别名)
	protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, String typeHandlerAlias) {
		if (typeHandlerAlias == null) {
			return null;
		}
		//根据别名获取对应的类
		Class<?> type = resolveClass(typeHandlerAlias);
		//若不是TypeHandler子类则报错
		if (type != null && !TypeHandler.class.isAssignableFrom(type)) {
			throw new BuilderException("Type " + type.getName()
					+ " is not a valid TypeHandler because it does not implement TypeHandler interface");
		}
		@SuppressWarnings("unchecked")
		Class<? extends TypeHandler<?>> typeHandlerType = (Class<? extends TypeHandler<?>>) type;
		//再调用另一个重载方法
		return resolveTypeHandler(javaType, typeHandlerType);
	}

	//解析类型处理器(根据java类型和类型处理器)
	protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, Class<? extends TypeHandler<?>> typeHandlerType) {
		if (typeHandlerType == null) {
			return null;
		}
		//去注册机上获取对应的类型处理器
		TypeHandler<?> handler = typeHandlerRegistry.getMappingTypeHandler(typeHandlerType);
		//若为空则新建一个类型处理器
		if (handler == null) {
			handler = typeHandlerRegistry.getInstance(javaType, typeHandlerType);
		}
		return handler;
	}

	//根据别名获取对应的类
	protected Class<?> resolveAlias(String alias) {
		return typeAliasRegistry.resolveAlias(alias);
	}
}
