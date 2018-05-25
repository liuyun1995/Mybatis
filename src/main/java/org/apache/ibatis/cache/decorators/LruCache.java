package org.apache.ibatis.cache.decorators;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

//最近最少使用缓存
public class LruCache implements Cache {

	private final Cache delegate;
	//额外用了一个map才做lru，但是委托的Cache里面其实也是一个map，这样等于用2倍的内存实现lru功能
	private Map<Object, Object> keyMap;
	private Object eldestKey;

	//构造器
	public LruCache(Cache delegate) {
		this.delegate = delegate;
		setSize(1024);
	}

	//获取缓存ID
	public String getId() {
		return delegate.getId();
	}

	//获取缓存大小
	public int getSize() {
		return delegate.getSize();
	}

	//设置缓存大小
	public void setSize(final int size) {
		keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {
			private static final long serialVersionUID = 4267176411845948333L;

			// 核心就是覆盖 LinkedHashMap.removeEldestEntry方法,
			// 返回true或false告诉 LinkedHashMap要不要删除此最老键值
			// LinkedHashMap内部其实就是每次访问或者插入一个元素都会把元素放到链表末尾，
			// 这样不经常访问的键值肯定就在链表开头啦
			@Override
			protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
				boolean tooBig = size() > size;
				if (tooBig) {
					// 这里没辙了，把eldestKey存入实例变量
					eldestKey = eldest.getKey();
				}
				return tooBig;
			}
		};
	}

	//放置对象
	public void putObject(Object key, Object value) {
		delegate.putObject(key, value);
		// 增加新纪录后，判断是否要将最老元素移除
		cycleKeyList(key);
	}

	//获取对象
	public Object getObject(Object key) {
		// get的时候调用一下LinkedHashMap.get，让经常访问的值移动到链表末尾
		keyMap.get(key); // touch
		return delegate.getObject(key);
	}

	//移除对象
	public Object removeObject(Object key) {
		return delegate.removeObject(key);
	}

	//清空缓存
	public void clear() {
		delegate.clear();
		keyMap.clear();
	}

	//获取读写锁
	public ReadWriteLock getReadWriteLock() {
		return null;
	}

	//放入对象同时移除最旧未使用的对象
	private void cycleKeyList(Object key) {
		keyMap.put(key, key);
		// keyMap是linkedhashmap，最老的记录已经被移除了，然后这里我们还需要移除被委托的那个cache的记录
		if (eldestKey != null) {
			delegate.removeObject(eldestKey);
			eldestKey = null;
		}
	}

}
