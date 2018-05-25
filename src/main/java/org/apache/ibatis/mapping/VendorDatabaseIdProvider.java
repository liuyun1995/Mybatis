package org.apache.ibatis.mapping;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.executor.BaseExecutor;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

//厂商数据库ID提供者
public class VendorDatabaseIdProvider implements DatabaseIdProvider {

	private static final Log log = LogFactory.getLog(BaseExecutor.class);

	private Properties properties;
	
	//设置属性
	public void setProperties(Properties p) {
		this.properties = p;
	}

	//根据数据源获取数据库名称
	public String getDatabaseId(DataSource dataSource) {
		if (dataSource == null) {
			throw new NullPointerException("dataSource cannot be null");
		}
		try {
			//根据数据源获取数据库名字
			return getDatabaseName(dataSource);
		} catch (Exception e) {
			log.error("Could not get a databaseId from dataSource", e);
		}
		return null;
	}

	//根据数据源获取数据库名字
	private String getDatabaseName(DataSource dataSource) throws SQLException {
		//获取数据库产品名称
		String productName = getDatabaseProductName(dataSource);
		//如果属性不为空
		if (this.properties != null) {
			//遍历所有属性
			for (Map.Entry<Object, Object> property : properties.entrySet()) {
				//如果存在属性键为数据库名, 则返回该属性值
				if (productName.contains((String) property.getKey())) {
					return (String) property.getValue();
				}
			}
			//如果属性中不存在数据库名, 则返回null
			return null;
		}
		//否则直接返回数据库名称
		return productName;
	}

	//根据数据源获取数据库产品名称
	private String getDatabaseProductName(DataSource dataSource) throws SQLException {
		Connection con = null;
		try {
			//获取数据库连接
			con = dataSource.getConnection();
			//获取数据库元数据
			DatabaseMetaData metaData = con.getMetaData();
			//获取数据库产品名称
			return metaData.getDatabaseProductName();
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					//ignored
				}
			}
		}
	}

}
