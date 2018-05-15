package org.apache.ibatis.mapping;

import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

public interface DatabaseIdProvider {

	void setProperties(Properties p);

	// 根据数据源来得到一个DB id
	String getDatabaseId(DataSource dataSource) throws SQLException;
	
}
