package org.apache.ibatis.cache;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.cache.decorators.TransactionalCache;

//事务缓存管理器, 被CachingExecutor使用
public class TransactionalCacheManager {

	//管理了许多TransactionalCache
	private Map<Cache, TransactionalCache> transactionalCaches = new HashMap<Cache, TransactionalCache>();

	public void clear(Cache cache) {
		getTransactionalCache(cache).clear();
	}

	//得到某个TransactionalCache的值
	public Object getObject(Cache cache, CacheKey key) {
		return getTransactionalCache(cache).getObject(key);
	}

	public void putObject(Cache cache, CacheKey key, Object value) {
		getTransactionalCache(cache).putObject(key, value);
	}

	//提交时全部提交
	public void commit() {
		for (TransactionalCache txCache : transactionalCaches.values()) {
			txCache.commit();
		}
	}

	//回滚时全部回滚
	public void rollback() {
		for (TransactionalCache txCache : transactionalCaches.values()) {
			txCache.rollback();
		}
	}

	private TransactionalCache getTransactionalCache(Cache cache) {
		TransactionalCache txCache = transactionalCaches.get(cache);
		if (txCache == null) {
			txCache = new TransactionalCache(cache);
			transactionalCaches.put(cache, txCache);
		}
		return txCache;
	}

}
