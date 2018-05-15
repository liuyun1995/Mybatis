package org.apache.ibatis.datasource.pooled;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;

//数据源连接池工厂 
public class PooledDataSourceFactory extends UnpooledDataSourceFactory {

	// 数据源换成了PooledDataSource
	public PooledDataSourceFactory() {
		this.dataSource = new PooledDataSource();
	}

}
