package org.apache.ibatis.executor.parameter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

//参数处理接口
public interface ParameterHandler {

	//获取参数
	Object getParameterObject();

	//设置参数
	void setParameters(PreparedStatement ps) throws SQLException;

}
