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

	//参数属性
	private static final String parameterProperties = "javaType,jdbcType,mode,numericScale,resultMap,typeHandler,jdbcTypeName";

	public SqlSourceBuilder(Configuration configuration) {
		super(configuration);
	}

	//解析原始SQL语句
	public SqlSource parse(String originalSql, Class<?> parameterType, Map<String, Object> additionalParameters) {
		//获取参数标记处理器
		ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler(configuration, parameterType, additionalParameters);
		//获取通用记号解析器
		GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
		//获取#{}里的参数
		String sql = parser.parse(originalSql);
		//返回静态的SqlSource
		return new StaticSqlSource(configuration, sql, handler.getParameterMappings());
	}

	//参数映射记号处理器
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
			//将参数字符串构建成参数映射
			parameterMappings.add(buildParameterMapping(content));
			//全部返回"?"
			return "?";
		}

		//构建参数映射
		private ParameterMapping buildParameterMapping(String content) {
			//将参数字符串解析成映射集合
			Map<String, String> propertiesMap = parseParameterMapping(content);
			//获取参数名
			String property = propertiesMap.get("property");
			Class<?> propertyType;
			
			//是否有指定的getter方法
			if (metaParameters.hasGetter(property)) {
				//获取getter方法的返回类型
				propertyType = metaParameters.getGetterType(property);
			//参数类型是否有对应类型处理器
			} else if (typeHandlerRegistry.hasTypeHandler(parameterType)) {
				propertyType = parameterType;
			//判断jdbc类型是否是CURSOR
			} else if (JdbcType.CURSOR.name().equals(propertiesMap.get("jdbcType"))) {
				propertyType = java.sql.ResultSet.class;
			//如果参数名不为空
			} else if (property != null) {
				MetaClass metaClass = MetaClass.forClass(parameterType);
				if (metaClass.hasGetter(property)) {
					propertyType = metaClass.getGetterType(property);
				} else {
					propertyType = Object.class;
				}
			} else {
				//否则参数类型为Object
				propertyType = Object.class;
			}
			//提供配置信息, 参数名以及参数的java类型来获得参数映射构建器
			ParameterMapping.Builder builder = new ParameterMapping.Builder(configuration, property, propertyType);
			
			Class<?> javaType = propertyType;
			String typeHandlerAlias = null;
			for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
				String name = entry.getKey();
				String value = entry.getValue();
				if ("javaType".equals(name)) {
					//根据别名获取javaType
					javaType = resolveClass(value);
					//设置javaType
					builder.javaType(javaType);
				} else if ("jdbcType".equals(name)) {
					//设置jdbcType
					builder.jdbcType(resolveJdbcType(value));
				} else if ("mode".equals(name)) {
					//设置参数模式
					builder.mode(resolveParameterMode(value));
				} else if ("numericScale".equals(name)) {
					//设置numericScale
					builder.numericScale(Integer.valueOf(value));
				} else if ("resultMap".equals(name)) {
					//设置resultMap
					builder.resultMapId(value);
				} else if ("typeHandler".equals(name)) {
					//获取类型处理器别名
					typeHandlerAlias = value;
				} else if ("jdbcTypeName".equals(name)) {
					//设置jdbcTypeName
					builder.jdbcTypeName(value);
				} else if ("property".equals(name)) {
					//不作处理
				} else if ("expression".equals(name)) {
					//暂时还不支持表达式
					throw new BuilderException("Expression based parameters are not supported yet");
				} else {
					//否则抛出异常
					throw new BuilderException("An invalid property '" + name + "' was found in mapping #{" + content
							+ "}.  Valid properties are " + parameterProperties);
				}
			}
			//例子：#{age,javaType=int,jdbcType=NUMERIC,typeHandler=MyTypeHandler}
			//如果指定了类型处理器, 就使用用户指定的类型处理器
			if (typeHandlerAlias != null) {
				//根据java类型和类型处理器别名来获取类型处理器
				builder.typeHandler(resolveTypeHandler(javaType, typeHandlerAlias));
			}
			//返回成功构建的参数映射
			return builder.build();
		}

		//根据字符串解析参数, 构建出一个Map
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
