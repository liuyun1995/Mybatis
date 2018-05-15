package org.apache.ibatis.cache.decorators;

import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * 定时调度缓存, 目的是每一小时清空一下缓存
 */
public class ScheduledCache implements Cache {

	private Cache delegate;
	protected long clearInterval;
	protected long lastClear;

	public ScheduledCache(Cache delegate) {
		this.delegate = delegate;
		// 1小时清空一次缓存
		this.clearInterval = 60 * 60 * 1000; // 1 hour
		this.lastClear = System.currentTimeMillis();
	}

	public void setClearInterval(long clearInterval) {
		this.clearInterval = clearInterval;
	}

	public String getId() {
		return delegate.getId();
	}

	public int getSize() {
		clearWhenStale();
		return delegate.getSize();
	}

	public void putObject(Object key, Object object) {
		clearWhenStale();
		delegate.putObject(key, object);
	}

	public Object getObject(Object key) {
		return clearWhenStale() ? null : delegate.getObject(key);
	}

	public Object removeObject(Object key) {
		clearWhenStale();
		return delegate.removeObject(key);
	}

	public void clear() {
		lastClear = System.currentTimeMillis();
		delegate.clear();
	}

	public ReadWriteLock getReadWriteLock() {
		return null;
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	private boolean clearWhenStale() {
		// 如果到时间了，清空一下缓存
		if (System.currentTimeMillis() - lastClear > clearInterval) {
			clear();
			return true;
		}
		return false;
	}

}
