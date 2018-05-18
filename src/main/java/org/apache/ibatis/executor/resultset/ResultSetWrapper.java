package org.apache.ibatis.executor.resultset;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.ObjectTypeHandler;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.apache.ibatis.type.UnknownTypeHandler;

//ResultSet包装类
class ResultSetWrapper {

	//结果集
	private final ResultSet resultSet;
	//类型处理器注册机
	private final TypeHandlerRegistry typeHandlerRegistry;
	//列名集合
	private final List<String> columnNames = new ArrayList<String>();
	//类名集合
	private final List<String> classNames = new ArrayList<String>();
	//jdbc类型集合
	private final List<JdbcType> jdbcTypes = new ArrayList<JdbcType>();
	//类型处理映射
	private final Map<String, Map<Class<?>, TypeHandler<?>>> typeHandlerMap = new HashMap<String, Map<Class<?>, TypeHandler<?>>>();
	
	private Map<String, List<String>> mappedColumnNamesMap = new HashMap<String, List<String>>();
	private Map<String, List<String>> unMappedColumnNamesMap = new HashMap<String, List<String>>();

	public ResultSetWrapper(ResultSet rs, Configuration configuration) throws SQLException {
		super();
		this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
		this.resultSet = rs;
		final ResultSetMetaData metaData = rs.getMetaData();
		final int columnCount = metaData.getColumnCount();
		for (int i = 1; i <= columnCount; i++) {
			columnNames.add(configuration.isUseColumnLabel() ? metaData.getColumnLabel(i) : metaData.getColumnName(i));
			jdbcTypes.add(JdbcType.forCode(metaData.getColumnType(i)));
			classNames.add(metaData.getColumnClassName(i));
		}
	}

	public ResultSet getResultSet() {
		return resultSet;
	}

	public List<String> getColumnNames() {
		return this.columnNames;
	}

	public List<String> getClassNames() {
		return Collections.unmodifiableList(classNames);
	}
	
	//获取类型处理器
	public TypeHandler<?> getTypeHandler(Class<?> propertyType, String columnName) {
		TypeHandler<?> handler = null;
		//根据列名获取类型处理器映射(二级缓存)
		Map<Class<?>, TypeHandler<?>> columnHandlers = typeHandlerMap.get(columnName);
		if (columnHandlers == null) {
			columnHandlers = new HashMap<Class<?>, TypeHandler<?>>();
			typeHandlerMap.put(columnName, columnHandlers);
		} else {
			//根据参数类型获取类型处理器
			handler = columnHandlers.get(propertyType);
		}
		if (handler == null) {
			//根据参数类型去类型处理器注册器上获取
			handler = typeHandlerRegistry.getTypeHandler(propertyType);
			if (handler == null || handler instanceof UnknownTypeHandler) {
				//在列名集合中找到该列名的位置
				final int index = columnNames.indexOf(columnName);
				//获取对应位置的jdbc类型
				final JdbcType jdbcType = jdbcTypes.get(index);
				//获取对应位置的java类型
				final Class<?> javaType = resolveClass(classNames.get(index));
				
				//若java类型和jdbc类型都不为空
				if (javaType != null && jdbcType != null) {
					handler = typeHandlerRegistry.getTypeHandler(javaType, jdbcType);
				//若只有java类型
				} else if (javaType != null) {
					handler = typeHandlerRegistry.getTypeHandler(javaType);
				//若只有jdbc类型
				} else if (jdbcType != null) {
					handler = typeHandlerRegistry.getTypeHandler(jdbcType);
				}
			}
			//若类型处理器仍为空, 或者是未知类型处理器, 则设置为Object类型处理器
			if (handler == null || handler instanceof UnknownTypeHandler) {
				handler = new ObjectTypeHandler();
			}
			//放入列处理器映射表中
			columnHandlers.put(propertyType, handler);
		}
		return handler;
	}

	private Class<?> resolveClass(String className) {
		try {
			return Resources.classForName(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	private void loadMappedAndUnmappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
		List<String> mappedColumnNames = new ArrayList<String>();
		List<String> unmappedColumnNames = new ArrayList<String>();
		final String upperColumnPrefix = columnPrefix == null ? null : columnPrefix.toUpperCase(Locale.ENGLISH);
		final Set<String> mappedColumns = prependPrefixes(resultMap.getMappedColumns(), upperColumnPrefix);
		for (String columnName : columnNames) {
			final String upperColumnName = columnName.toUpperCase(Locale.ENGLISH);
			if (mappedColumns.contains(upperColumnName)) {
				mappedColumnNames.add(upperColumnName);
			} else {
				unmappedColumnNames.add(columnName);
			}
		}
		mappedColumnNamesMap.put(getMapKey(resultMap, columnPrefix), mappedColumnNames);
		unMappedColumnNamesMap.put(getMapKey(resultMap, columnPrefix), unmappedColumnNames);
	}

	public List<String> getMappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
		List<String> mappedColumnNames = mappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
		if (mappedColumnNames == null) {
			loadMappedAndUnmappedColumnNames(resultMap, columnPrefix);
			mappedColumnNames = mappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
		}
		return mappedColumnNames;
	}

	public List<String> getUnmappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
		List<String> unMappedColumnNames = unMappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
		if (unMappedColumnNames == null) {
			loadMappedAndUnmappedColumnNames(resultMap, columnPrefix);
			unMappedColumnNames = unMappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
		}
		return unMappedColumnNames;
	}

	private String getMapKey(ResultMap resultMap, String columnPrefix) {
		return resultMap.getId() + ":" + columnPrefix;
	}

	private Set<String> prependPrefixes(Set<String> columnNames, String prefix) {
		if (columnNames == null || columnNames.isEmpty() || prefix == null || prefix.length() == 0) {
			return columnNames;
		}
		final Set<String> prefixed = new HashSet<String>();
		for (String columnName : columnNames) {
			prefixed.add(prefix + columnName);
		}
		return prefixed;
	}

}
