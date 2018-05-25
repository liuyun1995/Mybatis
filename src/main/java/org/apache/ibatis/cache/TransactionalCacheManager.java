package org.apache.ibatis.cache;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.cache.decorators.TransactionalCache;

//事务缓存管理器
public class TransactionalCacheManager {

	//事务缓存集合
	private Map<Cache, TransactionalCache> transactionalCaches = new HashMap<Cache, TransactionalCache>();

	//清空缓存
	public void clear(Cache cache) {
		getTransactionalCache(cache).clear();
	}

	//获取对象
	public Object getObject(Cache cache, CacheKey key) {
		return getTransactionalCache(cache).getObject(key);
	}

	//放置对象
	public void putObject(Cache cache, CacheKey key, Object value) {
		getTransactionalCache(cache).putObject(key, value);
	}

	//提交事务
	public void commit() {
		for (TransactionalCache txCache : transactionalCaches.values()) {
			txCache.commit();
		}
	}

	//回滚事务
	public void rollback() {
		for (TransactionalCache txCache : transactionalCaches.values()) {
			txCache.rollback();
		}
	}

	//获取事务缓存
	private TransactionalCache getTransactionalCache(Cache cache) {
		TransactionalCache txCache = transactionalCaches.get(cache);
		if (txCache == null) {
			txCache = new TransactionalCache(cache);
			transactionalCaches.put(cache, txCache);
		}
		return txCache;
	}

}
