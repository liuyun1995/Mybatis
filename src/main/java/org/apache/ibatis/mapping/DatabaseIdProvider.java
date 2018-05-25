package org.apache.ibatis.mapping;

import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

//数据库ID提供者
public interface DatabaseIdProvider {

	void setProperties(Properties p);
	
	String getDatabaseId(DataSource dataSource) throws SQLException;
	
}
