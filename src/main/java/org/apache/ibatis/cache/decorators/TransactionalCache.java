package org.apache.ibatis.cache.decorators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * 事务缓存, 一次性存入多个缓存，移除多个缓存
 */
public class TransactionalCache implements Cache {

	private Cache delegate;
	// commit时要不要清缓存
	private boolean clearOnCommit;
	// commit时要添加的元素
	private Map<Object, Object> entriesToAddOnCommit;
	private Set<Object> entriesMissedInCache;

	public TransactionalCache(Cache delegate) {
		this.delegate = delegate;
		// 默认commit时不清缓存
		this.clearOnCommit = false;
		this.entriesToAddOnCommit = new HashMap<Object, Object>();
		this.entriesMissedInCache = new HashSet<Object>();
	}

	public String getId() {
		return delegate.getId();
	}

	public int getSize() {
		return delegate.getSize();
	}

	public Object getObject(Object key) {
		// issue #116
		Object object = delegate.getObject(key);
		if (object == null) {
			entriesMissedInCache.add(key);
		}
		// issue #146
		if (clearOnCommit) {
			return null;
		} else {
			return object;
		}
	}

	public ReadWriteLock getReadWriteLock() {
		return null;
	}

	public void putObject(Object key, Object object) {
		entriesToAddOnCommit.put(key, object);
	}

	public Object removeObject(Object key) {
		return null;
	}

	public void clear() {
		clearOnCommit = true;
		entriesToAddOnCommit.clear();
	}

	// 多了commit方法，提供事务功能
	public void commit() {
		if (clearOnCommit) {
			delegate.clear();
		}
		flushPendingEntries();
		reset();
	}

	public void rollback() {
		unlockMissedEntries();
		reset();
	}

	private void reset() {
		clearOnCommit = false;
		entriesToAddOnCommit.clear();
		entriesMissedInCache.clear();
	}

	private void flushPendingEntries() {
		for (Map.Entry<Object, Object> entry : entriesToAddOnCommit.entrySet()) {
			delegate.putObject(entry.getKey(), entry.getValue());
		}
		for (Object entry : entriesMissedInCache) {
			if (!entriesToAddOnCommit.containsKey(entry)) {
				delegate.putObject(entry, null);
			}
		}
	}

	private void unlockMissedEntries() {
		for (Object entry : entriesMissedInCache) {
			delegate.putObject(entry, null);
		}
	}

}
