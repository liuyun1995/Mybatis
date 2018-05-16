package org.apache.ibatis.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;

//SQL源码构建器
public class SqlSourceBuilder extends BaseBuilder {

	private static final String parameterProperties = "javaType,jdbcType,mode,numericScale,resultMap,typeHandler,jdbcTypeName";

	public SqlSourceBuilder(Configuration configuration) {
		super(configuration);
	}

	public SqlSource parse(String originalSql, Class<?> parameterType, Map<String, Object> additionalParameters) {
		ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler(configuration, parameterType, additionalParameters);
		//替换#{}中间的部分
		GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
		String sql = parser.parse(originalSql);
		//返回静态SQL源码
		return new StaticSqlSource(configuration, sql, handler.getParameterMappings());
	}

	//参数映射记号处理器，静态内部类
	private static class ParameterMappingTokenHandler extends BaseBuilder implements TokenHandler {

		private List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();
		private Class<?> parameterType;
		private MetaObject metaParameters;

		public ParameterMappingTokenHandler(Configuration configuration, Class<?> parameterType,
				Map<String, Object> additionalParameters) {
			super(configuration);
			this.parameterType = parameterType;
			this.metaParameters = configuration.newMetaObject(additionalParameters);
		}

		public List<ParameterMapping> getParameterMappings() {
			return parameterMappings;
		}

		public String handleToken(String content) {
			// 先构建参数映射
			parameterMappings.add(buildParameterMapping(content));
			// 如何替换很简单，永远是一个问号，但是参数的信息要记录在parameterMappings里面供后续使用
			return "?";
		}

		// 构建参数映射
		private ParameterMapping buildParameterMapping(String content) {
			//例子：#{favouriteSection,jdbcType=VARCHAR}
			//先解析参数映射,就是转化成一个hashmap
			Map<String, String> propertiesMap = parseParameterMapping(content);
			String property = propertiesMap.get("property");
			Class<?> propertyType;
			//这里分支比较多，需要逐个理解
			if (metaParameters.hasGetter(property)) {
				propertyType = metaParameters.getGetterType(property);
			} else if (typeHandlerRegistry.hasTypeHandler(parameterType)) {
				propertyType = parameterType;
			} else if (JdbcType.CURSOR.name().equals(propertiesMap.get("jdbcType"))) {
				propertyType = java.sql.ResultSet.class;
			} else if (property != null) {
				MetaClass metaClass = MetaClass.forClass(parameterType);
				if (metaClass.hasGetter(property)) {
					propertyType = metaClass.getGetterType(property);
				} else {
					propertyType = Object.class;
				}
			} else {
				propertyType = Object.class;
			}
			//提供配置信息, 参数名以及参数的java类型来获得参数映射构建器
			ParameterMapping.Builder builder = new ParameterMapping.Builder(configuration, property, propertyType);
			Class<?> javaType = propertyType;
			String typeHandlerAlias = null;
			// 例子：#{favouriteSection,jdbcType=VARCHAR}
			// 遍历propertiesMap, 在这个Map存放的类似于{key=jdbcType,value=VARCHAR}这样的映射
			for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
				String name = entry.getKey();
				String value = entry.getValue();
				if ("javaType".equals(name)) {
					// 根据别名去找到对应的类
					javaType = resolveClass(value);
					builder.javaType(javaType);
				} else if ("jdbcType".equals(name)) {
					// 根据别名去找到对应的jdbcType
					builder.jdbcType(resolveJdbcType(value));
				} else if ("mode".equals(name)) {
					builder.mode(resolveParameterMode(value));
				} else if ("numericScale".equals(name)) {
					builder.numericScale(Integer.valueOf(value));
				} else if ("resultMap".equals(name)) {
					builder.resultMapId(value);
				} else if ("typeHandler".equals(name)) {
					typeHandlerAlias = value;
				} else if ("jdbcTypeName".equals(name)) {
					builder.jdbcTypeName(value);
				} else if ("property".equals(name)) {
					// Do Nothing
				} else if ("expression".equals(name)) {
					// 表达式现在还不支持
					throw new BuilderException("Expression based parameters are not supported yet");
				} else {
					throw new BuilderException("An invalid property '" + name + "' was found in mapping #{" + content
							+ "}.  Valid properties are " + parameterProperties);
				}
			}
			// 例子：#{age,javaType=int,jdbcType=NUMERIC,typeHandler=MyTypeHandler}
			// 如果指定了类型处理器, 就使用用户指定的类型处理器
			if (typeHandlerAlias != null) {
				// 根据java类型和类型处理器别名来获取到类型处理器
				builder.typeHandler(resolveTypeHandler(javaType, typeHandlerAlias));
			}
			return builder.build(); //到这里成功构建参数映射,返回ParameterMapping
		}

		// 根据字符串解析参数, 构建出一个Map
		private Map<String, String> parseParameterMapping(String content) {
			try {
				return new ParameterExpression(content);
			} catch (BuilderException ex) {
				throw ex;
			} catch (Exception ex) {
				throw new BuilderException("Parsing error was found in mapping #{" + content
						+ "}.  Check syntax #{property|(expression), var1=value1, var2=value2, ...} ", ex);
			}
		}
	}

}
