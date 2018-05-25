package org.apache.ibatis.cache.decorators;

import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

//定时调度缓存, 目的是每一小时清空一下缓存
public class ScheduledCache implements Cache {

	private Cache delegate;         //缓存代表
	protected long clearInterval;  //清空间隔
	protected long lastClear;      //最后一次清空时间戳

	//构造器
	public ScheduledCache(Cache delegate) {
		this.delegate = delegate;
		this.clearInterval = 60 * 60 * 1000;         //1小时清一次
		this.lastClear = System.currentTimeMillis();
	}

	//设置清空间隔
	public void setClearInterval(long clearInterval) {
		this.clearInterval = clearInterval;
	}

	//获取缓存ID
	public String getId() {
		return delegate.getId();
	}

	//获取缓存大小
	public int getSize() {
		clearWhenStale();
		return delegate.getSize();
	}

	//放置对象
	public void putObject(Object key, Object object) {
		clearWhenStale();
		delegate.putObject(key, object);
	}

	//获取对象
	public Object getObject(Object key) {
		return clearWhenStale() ? null : delegate.getObject(key);
	}

	//移除对象
	public Object removeObject(Object key) {
		clearWhenStale();
		return delegate.removeObject(key);
	}

	//清空缓存
	public void clear() {
		lastClear = System.currentTimeMillis();
		delegate.clear();
	}

	//获取读写锁
	public ReadWriteLock getReadWriteLock() {
		return null;
	}
	
	//定时清空缓存
	private boolean clearWhenStale() {
		if (System.currentTimeMillis() - lastClear > clearInterval) {
			clear();
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

}
