package org.apache.ibatis.cache.decorators;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;
import org.apache.ibatis.io.Resources;

//序列化缓存, 用途是先将对象序列化成2进制，再缓存,好处是将对象压缩了，省内存 坏处是速度慢了
public class SerializedCache implements Cache {

	private Cache delegate;   //缓存代表

	//构造器
	public SerializedCache(Cache delegate) {
		this.delegate = delegate;
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
		if (object == null || object instanceof Serializable) {
			// 先序列化，再委托被包装者putObject
			delegate.putObject(key, serialize((Serializable) object));
		} else {
			throw new CacheException("SharedCache failed to make a copy of a non-serializable object: " + object);
		}
	}

	//获取对象
	public Object getObject(Object key) {
		// 先委托被包装者getObject,再反序列化
		Object object = delegate.getObject(key);
		return object == null ? null : deserialize((byte[]) object);
	}

	//移除对象
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

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	//序列化方法
	private byte[] serialize(Serializable value) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(value);
			oos.flush();
			oos.close();
			return bos.toByteArray();
		} catch (Exception e) {
			throw new CacheException("Error serializing object.  Cause: " + e, e);
		}
	}

	//反序列化方法
	private Serializable deserialize(byte[] value) {
		Serializable result;
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(value);
			ObjectInputStream ois = new CustomObjectInputStream(bis);
			result = (Serializable) ois.readObject();
			ois.close();
		} catch (Exception e) {
			throw new CacheException("Error deserializing object.  Cause: " + e, e);
		}
		return result;
	}

	//这个Custom不明白何意
	public static class CustomObjectInputStream extends ObjectInputStream {

		public CustomObjectInputStream(InputStream in) throws IOException {
			super(in);
		}

		@Override
		protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
			return Resources.classForName(desc.getName());
		}

	}

}
