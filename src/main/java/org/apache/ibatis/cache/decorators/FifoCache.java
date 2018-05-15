package org.apache.ibatis.cache.decorators;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * FIFO缓存 这个类就是维护一个FIFO链表，其他都委托给所包装的cache去做。典型的装饰模式
 */
public class FifoCache implements Cache {

	private final Cache delegate;
	private Deque<Object> keyList;
	private int size;

	public FifoCache(Cache delegate) {
		this.delegate = delegate;
		this.keyList = new LinkedList<Object>();
		this.size = 1024;
	}

	public String getId() {
		return delegate.getId();
	}

	public int getSize() {
		return delegate.getSize();
	}

	public void setSize(int size) {
		this.size = size;
	}

	public void putObject(Object key, Object value) {
		cycleKeyList(key);
		delegate.putObject(key, value);
	}

	public Object getObject(Object key) {
		return delegate.getObject(key);
	}

	public Object removeObject(Object key) {
		return delegate.removeObject(key);
	}

	public void clear() {
		delegate.clear();
		keyList.clear();
	}

	public ReadWriteLock getReadWriteLock() {
		return null;
	}

	private void cycleKeyList(Object key) {
		// 增加记录时判断如果记录已超过1024条，会移除链表的第一个元素，从而达到FIFO缓存效果
		keyList.addLast(key);
		if (keyList.size() > size) {
			Object oldestKey = keyList.removeFirst();
			delegate.removeObject(oldestKey);
		}
	}

}
