package org.apache.ibatis.cache.decorators;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;

//块型缓存
public class BlockingCache implements Cache {

	//超时时间
	private long timeout;
	//缓存代表
	private final Cache delegate;
	//锁集合
	private final ConcurrentHashMap<Object, ReentrantLock> locks;

	//构造器
	public BlockingCache(Cache delegate) {
		this.delegate = delegate;
		this.locks = new ConcurrentHashMap<Object, ReentrantLock>();
	}

	//获取缓存ID
	public String getId() {
		return delegate.getId();
	}

	//获取缓存大小
	public int getSize() {
		return delegate.getSize();
	}

	//放置对象
	public void putObject(Object key, Object value) {
		try {
			delegate.putObject(key, value);
		} finally {
			releaseLock(key);
		}
	}

	//获取对象
	public Object getObject(Object key) {
		acquireLock(key);
		Object value = delegate.getObject(key);
		if (value != null) {
			releaseLock(key);
		}
		return value;
	}

	//删除对象
	public Object removeObject(Object key) {
		return delegate.removeObject(key);
	}

	//清空缓存
	public void clear() {
		delegate.clear();
	}

	//获取读写锁
	public ReadWriteLock getReadWriteLock() {
		return null;
	}

	//获取键上的锁
	private ReentrantLock getLockForKey(Object key) {
		ReentrantLock lock = new ReentrantLock();
		ReentrantLock previous = locks.putIfAbsent(key, lock);
		return previous == null ? lock : previous;
	}

	//请求锁
	private void acquireLock(Object key) {
		Lock lock = getLockForKey(key);
		if (timeout > 0) {
			try {
				boolean acquired = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
				if (!acquired) {
					throw new CacheException("Couldn't get a lock in " + timeout + " for the key " + key
							+ " at the cache " + delegate.getId());
				}
			} catch (InterruptedException e) {
				throw new CacheException("Got interrupted while trying to acquire lock for key " + key, e);
			}
		} else {
			lock.lock();
		}
	}

	//释放锁
	private void releaseLock(Object key) {
		ReentrantLock lock = locks.get(key);
		if (lock.isHeldByCurrentThread()) {
			lock.unlock();
		}
	}

	//获取超时时间
	public long getTimeout() {
		return timeout;
	}

	//设置超时时间
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
}