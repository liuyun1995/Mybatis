package org.apache.ibatis.reflection.wrapper;

import org.apache.ibatis.reflection.MetaObject;

//对象包装器工厂
public interface ObjectWrapperFactory {

	//指定对象是否存在包装类
	boolean hasWrapperFor(Object object);

	//获取指定对象的包装类
	ObjectWrapper getWrapperFor(MetaObject metaObject, Object object);

}
