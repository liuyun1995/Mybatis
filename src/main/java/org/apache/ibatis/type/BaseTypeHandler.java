package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.session.Configuration;

//类型处理器的基类
public abstract class BaseTypeHandler<T> extends TypeReference<T> implements TypeHandler<T> {

	protected Configuration configuration;

	//设置配置信息
	public void setConfiguration(Configuration c) {
		this.configuration = c;
	}

	//设置参数
	public void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
		//特殊情况, 设置NULL
		if (parameter == null) {
			if (jdbcType == null) {
				//如果没设置jdbcType, 报错啦
				throw new TypeException(
						"JDBC requires that the JdbcType must be specified for all nullable parameters.");
			}
			try {
				//设成NULL
				ps.setNull(i, jdbcType.TYPE_CODE);
			} catch (SQLException e) {
				throw new TypeException("Error setting null for parameter #" + i + " with JdbcType " + jdbcType + " . "
						+ "Try setting a different JdbcType for this parameter or a different jdbcTypeForNull configuration property. "
						+ "Cause: " + e, e);
			}
		} else {
			//非NULL情况, 怎么设还得交给不同的子类完成, setNonNullParameter是一个抽象方法
			setNonNullParameter(ps, i, parameter, jdbcType);
		}
	}

	//根据列名获取结果
	public T getResult(ResultSet rs, String columnName) throws SQLException {
		//调用子类的方法, 获取可为空的结果对象
		T result = getNullableResult(rs, columnName);
		if (rs.wasNull()) {
			return null;
		} else {
			return result;
		}
	}

	//根据列索引获取结果
	public T getResult(ResultSet rs, int columnIndex) throws SQLException {
		T result = getNullableResult(rs, columnIndex);
		if (rs.wasNull()) {
			return null;
		} else {
			return result;
		}
	}

	//获取结果对象
	public T getResult(CallableStatement cs, int columnIndex) throws SQLException {
		T result = getNullableResult(cs, columnIndex);
		if (cs.wasNull()) {
			return null;
		} else {
			return result;
		}
	}

	//非NULL情况，怎么设参数还得交给不同的子类完成
	public abstract void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;

	//获取可为空的结果对象(根据列名)
	public abstract T getNullableResult(ResultSet rs, String columnName) throws SQLException;

	//获取可为空的结果对象(根据列索引)
	public abstract T getNullableResult(ResultSet rs, int columnIndex) throws SQLException;

	//获取可为空的结果对象
	public abstract T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException;

}
