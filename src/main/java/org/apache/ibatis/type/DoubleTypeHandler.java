package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DoubleTypeHandler extends BaseTypeHandler<Double> {

	//设置非空参数
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, Double parameter, JdbcType jdbcType)
			throws SQLException {
		ps.setDouble(i, parameter);
	}

	//获取可为空的结果对象(根据结果集和列名)
	@Override
	public Double getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return rs.getDouble(columnName);
	}

	//获取可为空的结果对象(根据结果集和列索引)
	@Override
	public Double getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return rs.getDouble(columnIndex);
	}

	//获取结果对象
	@Override
	public Double getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return cs.getDouble(columnIndex);
	}

}
