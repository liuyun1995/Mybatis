package org.apache.ibatis.cache.decorators;

import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

//日志缓存 添加功能：取缓存时打印命中率
public class LoggingCache implements Cache {
	
	private Log log;
	private Cache delegate;
	protected int requests = 0;
	protected int hits = 0;

	//构造器
	public LoggingCache(Cache delegate) {
		this.delegate = delegate;
		this.log = LogFactory.getLog(getId());
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
	public void putObject(Object key, Object object) {
		delegate.putObject(key, object);
	}

	//获取对象
	public Object getObject(Object key) {
		//访问数量加一
		requests++;
		//获取缓存对象
		final Object value = delegate.getObject(key);
		//若value不为空则表示命中
		if (value != null) {
			//命中数加一
			hits++;
		}
		if (log.isDebugEnabled()) {
			//通过日志打印命中率
			log.debug("Cache Hit Ratio [" + getId() + "]: " + getHitRatio());
		}
		//返回缓存对象
		return value;
	}

	//移除缓存
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
	
	//获取缓存命中率
	private double getHitRatio() {
		return (double) hits / (double) requests;
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
