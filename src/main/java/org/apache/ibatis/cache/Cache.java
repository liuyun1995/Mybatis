package org.apache.ibatis.cache;

import java.util.concurrent.locks.ReadWriteLock;

//缓存接口
public interface Cache {
	
	//获取id
	String getId();
	
	//放置对象
	void putObject(Object key, Object value);
	
	//获取对象
	Object getObject(Object key);
	
	//删除对象
	Object removeObject(Object key);
	
	//清空缓存
	void clear();
	
	//获取大小
	int getSize();
	
	//获取读写锁
	ReadWriteLock getReadWriteLock();
	
}