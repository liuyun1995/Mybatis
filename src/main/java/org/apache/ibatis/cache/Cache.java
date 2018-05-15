package org.apache.ibatis.cache;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * 缓存
 */
public interface Cache {

	// 取得ID
	String getId();

	// 存入值
	void putObject(Object key, Object value);

	// 获取值
	Object getObject(Object key);

	// 删除值
	Object removeObject(Object key);

	// 清空
	void clear();
	
	// 取得大小
	int getSize();
	
	// 取得读写锁, 从3.2.6开始没用了，要SPI自己实现锁
	ReadWriteLock getReadWriteLock();

}