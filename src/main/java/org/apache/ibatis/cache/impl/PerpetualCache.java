package org.apache.ibatis.cache.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;

/**
 * 永久缓存, 一旦存入就一直保持
 */
public class PerpetualCache implements Cache {

	// 每个永久缓存有一个ID来识别
	private String id;

	// 内部就是一个HashMap,所有方法基本就是直接调用HashMap的方法,不支持多线程？
	private Map<Object, Object> cache = new HashMap<Object, Object>();

	public PerpetualCache(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public int getSize() {
		return cache.size();
	}

	public void putObject(Object key, Object value) {
		cache.put(key, value);
	}

	public Object getObject(Object key) {
		return cache.get(key);
	}

	public Object removeObject(Object key) {
		return cache.remove(key);
	}

	public void clear() {
		cache.clear();
	}

	public ReadWriteLock getReadWriteLock() {
		return null;
	}

	@Override
	public boolean equals(Object o) {
		// 只要id相等就认为两个cache相同
		if (getId() == null) {
			throw new CacheException("Cache instances require an ID.");
		}
		if (this == o) {
			return true;
		}
		if (!(o instanceof Cache)) {
			return false;
		}

		Cache otherCache = (Cache) o;
		return getId().equals(otherCache.getId());
	}

	@Override
	public int hashCode() {
		if (getId() == null) {
			throw new CacheException("Cache instances require an ID.");
		}
		return getId().hashCode();
	}

}
