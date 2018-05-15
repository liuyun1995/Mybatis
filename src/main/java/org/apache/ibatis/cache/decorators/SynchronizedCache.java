package org.apache.ibatis.cache.decorators;

import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * 同步缓, 防止多线程问题 核心: 加锁 ReadWriteLock.readLock().lock()/unlock()
 * ReadWriteLock.writeLock().lock()/unlock()
 * 
 * 3.2.6以后这个类已经没用了，考虑到Hazelcast, EhCache已经有锁机制了，所以这个锁就画蛇添足了。
 * bug见https://github.com/mybatis/mybatis-3/issues/159
 */
public class SynchronizedCache implements Cache {

	private Cache delegate;

	public SynchronizedCache(Cache delegate) {
		this.delegate = delegate;
	}

	public String getId() {
		return delegate.getId();
	}

	public synchronized int getSize() {
		return delegate.getSize();
	}

	public synchronized void putObject(Object key, Object object) {
		delegate.putObject(key, object);
	}

	public synchronized Object getObject(Object key) {
		return delegate.getObject(key);
	}

	public synchronized Object removeObject(Object key) {
		return delegate.removeObject(key);
	}

	public synchronized void clear() {
		delegate.clear();
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	public ReadWriteLock getReadWriteLock() {
		return null;
	}

}
