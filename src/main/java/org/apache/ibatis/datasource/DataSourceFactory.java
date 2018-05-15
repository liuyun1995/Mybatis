package org.apache.ibatis.datasource;

import java.util.Properties;
import javax.sql.DataSource;

/**
 * 数据源工厂, 有三种内建的数据源类型 UNPOOLED POOLED JNDI
 */
public interface DataSourceFactory {

	// 设置属性,被XMLConfigBuilder所调用
	void setProperties(Properties props);

	// 生产数据源,直接得到javax.sql.DataSource
	DataSource getDataSource();

}
