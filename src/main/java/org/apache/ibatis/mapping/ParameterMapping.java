package org.apache.ibatis.mapping;

import java.sql.ResultSet;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

//参数映射, 对应一个参数表达式
//例子：#{property,javaType=int,jdbcType=NUMERIC}
public class ParameterMapping {

	private Configuration configuration;

	private String property;                   //参数名
	private ParameterMode mode;                //参数模式
	private Class<?> javaType = Object.class; //参数java类型
	private JdbcType jdbcType;                //jdbc类型
	private Integer numericScale;             //numericScale
	private TypeHandler<?> typeHandler;       //类型处理器
	private String resultMapId;               //结果映射ID
	private String jdbcTypeName;              //jdbc类型名
	private String expression;                //表达式

	private ParameterMapping() {}

	// 静态内部类，建造者模式
	public static class Builder {
		private ParameterMapping parameterMapping = new ParameterMapping();

		public Builder(Configuration configuration, String property, TypeHandler<?> typeHandler) {
			parameterMapping.configuration = configuration;
			parameterMapping.property = property;
			parameterMapping.typeHandler = typeHandler;
			parameterMapping.mode = ParameterMode.IN;
		}

		public Builder(Configuration configuration, String property, Class<?> javaType) {
			parameterMapping.configuration = configuration;
			parameterMapping.property = property;
			parameterMapping.javaType = javaType;
			parameterMapping.mode = ParameterMode.IN;
		}

		public Builder mode(ParameterMode mode) {
			parameterMapping.mode = mode;
			return this;
		}

		public Builder javaType(Class<?> javaType) {
			parameterMapping.javaType = javaType;
			return this;
		}

		public Builder jdbcType(JdbcType jdbcType) {
			parameterMapping.jdbcType = jdbcType;
			return this;
		}

		public Builder numericScale(Integer numericScale) {
			parameterMapping.numericScale = numericScale;
			return this;
		}

		public Builder resultMapId(String resultMapId) {
			parameterMapping.resultMapId = resultMapId;
			return this;
		}

		public Builder typeHandler(TypeHandler<?> typeHandler) {
			parameterMapping.typeHandler = typeHandler;
			return this;
		}

		public Builder jdbcTypeName(String jdbcTypeName) {
			parameterMapping.jdbcTypeName = jdbcTypeName;
			return this;
		}

		public Builder expression(String expression) {
			parameterMapping.expression = expression;
			return this;
		}

		//构建方法
		public ParameterMapping build() {
			resolveTypeHandler();     //1.确定类型处理器
			validate();               //2.进行验证
			return parameterMapping; //3.返回构造完毕的参数映射器
		}

		private void validate() {
			if (ResultSet.class.equals(parameterMapping.javaType)) {
				if (parameterMapping.resultMapId == null) {
					throw new IllegalStateException("Missing resultmap in property '" + parameterMapping.property
							+ "'.  " + "Parameters of type java.sql.ResultSet require a resultmap.");
				}
			} else {
				if (parameterMapping.typeHandler == null) {
					throw new IllegalStateException("Type handler was null on parameter mapping for property '"
							+ parameterMapping.property + "'.  "
							+ "It was either not specified and/or could not be found for the javaType / jdbcType combination specified.");
				}
			}
		}

		//确定类型处理器方法
		private void resolveTypeHandler() {
			//如果没有指定特殊的typeHandler，则根据javaType，jdbcType来查表确定一个默认的typeHandler
			if (parameterMapping.typeHandler == null && parameterMapping.javaType != null) {
				Configuration configuration = parameterMapping.configuration;
				TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
				parameterMapping.typeHandler = typeHandlerRegistry.getTypeHandler(parameterMapping.javaType,
						parameterMapping.jdbcType);
			}
		}

	}

	public String getProperty() {
		return property;
	}
	
	public ParameterMode getMode() {
		return mode;
	}

	public Class<?> getJavaType() {
		return javaType;
	}
	
	public JdbcType getJdbcType() {
		return jdbcType;
	}
	
	public Integer getNumericScale() {
		return numericScale;
	}
	
	public TypeHandler<?> getTypeHandler() {
		return typeHandler;
	}
	
	public String getResultMapId() {
		return resultMapId;
	}
	
	public String getJdbcTypeName() {
		return jdbcTypeName;
	}
	
	public String getExpression() {
		return expression;
	}

}
