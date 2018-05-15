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

/**
 * 厂商数据库Id提供者
 */
public class VendorDatabaseIdProvider implements DatabaseIdProvider {

	private static final Log log = LogFactory.getLog(BaseExecutor.class);

	private Properties properties;

	public String getDatabaseId(DataSource dataSource) {
		if (dataSource == null) {
			throw new NullPointerException("dataSource cannot be null");
		}
		try {
			// 根据dataSource得到数据库名字
			return getDatabaseName(dataSource);
		} catch (Exception e) {
			log.error("Could not get a databaseId from dataSource", e);
		}
		return null;
	}

	public void setProperties(Properties p) {
		this.properties = p;
	}

	private String getDatabaseName(DataSource dataSource) throws SQLException {
		// 先得到productName
		String productName = getDatabaseProductName(dataSource);
		if (this.properties != null) {
			// 如果设置了缩写properties，则一个个比较返回匹配的缩写
			for (Map.Entry<Object, Object> property : properties.entrySet()) {
				if (productName.contains((String) property.getKey())) {
					return (String) property.getValue();
				}
			}
			// no match, return null
			return null;
		}
		return productName;
	}

	private String getDatabaseProductName(DataSource dataSource) throws SQLException {
		Connection con = null;
		try {
			con = dataSource.getConnection();
			// 核心就是DatabaseMetaData.getDatabaseProductName()得到数据库产品名字
			DatabaseMetaData metaData = con.getMetaData();
			return metaData.getDatabaseProductName();
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					// ignored
				}
			}
		}
	}

}
