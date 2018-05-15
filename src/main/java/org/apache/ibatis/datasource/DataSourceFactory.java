package org.apache.ibatis.datasource;

import java.util.Properties;
import javax.sql.DataSource;

//数据源工厂
public interface DataSourceFactory {

	//设置属性,被XMLConfigBuilder所调用
	void setProperties(Properties props);

	//获取数据源
	DataSource getDataSource();

}
