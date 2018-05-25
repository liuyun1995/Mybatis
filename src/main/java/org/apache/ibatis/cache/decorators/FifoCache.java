package org.apache.ibatis.cache.decorators;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

//先进先出缓存
public class FifoCache implements Cache {

	private final Cache delegate;
	private Deque<Object> keyList;
	private int size;

	//构造器
	public FifoCache(Cache delegate) {
		this.delegate = delegate;
		this.keyList = new LinkedList<Object>();
		this.size = 1024;
	}

	//获取缓存ID
	public String getId() {
		return delegate.getId();
	}

	//获取缓存大小
	public int getSize() {
		return delegate.getSize();
	}

	//设置缓存大小
	public void setSize(int size) {
		this.size = size;
	}

	//放置对象
	public void putObject(Object key, Object value) {
		cycleKeyList(key);
		delegate.putObject(key, value);
	}

	//获取对象
	public Object getObject(Object key) {
		return delegate.getObject(key);
	}

	//删除对象
	public Object removeObject(Object key) {
		return delegate.removeObject(key);
	}

	//清空缓存
	public void clear() {
		delegate.clear();
		keyList.clear();
	}

	//获取读写锁
	public ReadWriteLock getReadWriteLock() {
		return null;
	}

	//添加对象同时移除第一个对象
	private void cycleKeyList(Object key) {
		keyList.addLast(key);
		if (keyList.size() > size) {
			Object oldestKey = keyList.removeFirst();
			delegate.removeObject(oldestKey);
		}
	}

}
