package org.apache.ibatis.cache;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

//缓存key
public class CacheKey implements Cloneable, Serializable {

	private static final long serialVersionUID = 1146682552656046210L;

	public static final CacheKey NULL_CACHE_KEY = new NullCacheKey();

	private static final int DEFAULT_MULTIPLYER = 37;
	private static final int DEFAULT_HASHCODE = 17;

	private int multiplier;            //乘数
	private int hashcode;              //哈希码
	private long checksum;             //校验码
	private int count;                 //计数值
	private List<Object> updateList;   //更新列表

	public CacheKey() {
		this.hashcode = DEFAULT_HASHCODE;
		this.multiplier = DEFAULT_MULTIPLYER;
		this.count = 0;
		this.updateList = new ArrayList<Object>();
	}

	// 传入一个Object数组，更新hashcode和效验码
	public CacheKey(Object[] objects) {
		this();
		updateAll(objects);
	}

	public int getUpdateCount() {
		return updateList.size();
	}

	public void update(Object object) {
		if (object != null && object.getClass().isArray()) {
			// 如果是数组，则循环调用doUpdate
			int length = Array.getLength(object);
			for (int i = 0; i < length; i++) {
				Object element = Array.get(object, i);
				doUpdate(element);
			}
		} else {
			// 否则，doUpdate
			doUpdate(object);
		}
	}

	//执行更新方法
	private void doUpdate(Object object) {
		// 计算hash值，校验码
		int baseHashCode = object == null ? 1 : object.hashCode();

		count++;
		checksum += baseHashCode;
		baseHashCode *= count;

		hashcode = multiplier * hashcode + baseHashCode;

		// 同时将对象加入列表，这样万一两个CacheKey的hash码碰巧一样，再根据对象严格equals来区分
		updateList.add(object);
	}

	//更新所有对象
	public void updateAll(Object[] objects) {
		for (Object o : objects) {
			update(o);
		}
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof CacheKey)) {
			return false;
		}

		final CacheKey cacheKey = (CacheKey) object;

		// 先比hashcode，checksum，count，理论上可以快速比出来
		if (hashcode != cacheKey.hashcode) {
			return false;
		}
		if (checksum != cacheKey.checksum) {
			return false;
		}
		if (count != cacheKey.count) {
			return false;
		}

		// 万一两个CacheKey的hash码碰巧一样，再根据对象严格equals来区分
		// 这里两个list的size没比是否相等，其实前面count相等就已经保证了
		for (int i = 0; i < updateList.size(); i++) {
			Object thisObject = updateList.get(i);
			Object thatObject = cacheKey.updateList.get(i);
			if (thisObject == null) {
				if (thatObject != null) {
					return false;
				}
			} else {
				if (!thisObject.equals(thatObject)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return hashcode;
	}

	@Override
	public String toString() {
		StringBuilder returnValue = new StringBuilder().append(hashcode).append(':').append(checksum);
		for (int i = 0; i < updateList.size(); i++) {
			returnValue.append(':').append(updateList.get(i));
		}

		return returnValue.toString();
	}

	@Override
	public CacheKey clone() throws CloneNotSupportedException {
		CacheKey clonedCacheKey = (CacheKey) super.clone();
		clonedCacheKey.updateList = new ArrayList<Object>(updateList);
		return clonedCacheKey;
	}

}
