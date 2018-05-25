package org.apache.ibatis.cache.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;

//永久缓存
public class PerpetualCache implements Cache {

	//缓存ID
	private String id;
	//缓存对象
	private Map<Object, Object> cache = new HashMap<Object, Object>();

	//构造器
	public PerpetualCache(String id) {
		this.id = id;
	}

	//获取缓存ID
	public String getId() {
		return id;
	}

	//获取缓存大小
	public int getSize() {
		return cache.size();
	}

	//放置对象
	public void putObject(Object key, Object value) {
		cache.put(key, value);
	}

	//获取对象
	public Object getObject(Object key) {
		return cache.get(key);
	}

	//删除对象
	public Object removeObject(Object key) {
		return cache.remove(key);
	}

	//清空缓存
	public void clear() {
		cache.clear();
	}

	//获取读写锁
	public ReadWriteLock getReadWriteLock() {
		return null;
	}

	@Override
	public boolean equals(Object o) {
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
