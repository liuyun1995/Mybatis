package org.apache.ibatis.reflection.factory;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.ibatis.reflection.ReflectionException;

//默认对象工厂
public class DefaultObjectFactory implements ObjectFactory, Serializable {

	private static final long serialVersionUID = -8855120656740914948L;

	//根据类型新建对象
	public <T> T create(Class<T> type) {
		return create(type, null, null);
	}

	//新建对象方法
	@SuppressWarnings("unchecked")
	public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
		//1.解析接口
		Class<?> classToCreate = resolveInterface(type);
		//2.实例化类
		return (T) instantiateClass(classToCreate, constructorArgTypes, constructorArgs);
	}
	
	//1.解析接口,将interface转为实际class
	protected Class<?> resolveInterface(Class<?> type) {
		Class<?> classToCreate;
		if (type == List.class || type == Collection.class || type == Iterable.class) {
			//List|Collection|Iterable-->ArrayList
			classToCreate = ArrayList.class;
		} else if (type == Map.class) {
			//Map->HashMap
			classToCreate = HashMap.class;
		} else if (type == SortedSet.class) {
			//SortedSet->TreeSet
			classToCreate = TreeSet.class;
		} else if (type == Set.class) {
			//Set->HashSet
			classToCreate = HashSet.class;
		} else {
			//除此以外，就用原来的类型
			classToCreate = type;
		}
		return classToCreate;
	}

	//2.实例化类
	private <T> T instantiateClass(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
		try {
			Constructor<T> constructor;
			//如果没有传入constructor就调用空构造函数
			if (constructorArgTypes == null || constructorArgs == null) {
				constructor = type.getDeclaredConstructor();
				if (!constructor.isAccessible()) {
					constructor.setAccessible(true);
				}
				return constructor.newInstance();
			}
			//如果传入constructor则调用传入的构造函数
			constructor = type.getDeclaredConstructor(constructorArgTypes.toArray(new Class[constructorArgTypes.size()]));
			if (!constructor.isAccessible()) {
				constructor.setAccessible(true);
			}
			return constructor.newInstance(constructorArgs.toArray(new Object[constructorArgs.size()]));
		} catch (Exception e) {
			StringBuilder argTypes = new StringBuilder();
			if (constructorArgTypes != null) {
				for (Class<?> argType : constructorArgTypes) {
					argTypes.append(argType.getSimpleName());
					argTypes.append(",");
				}
			}
			StringBuilder argValues = new StringBuilder();
			if (constructorArgs != null) {
				for (Object argValue : constructorArgs) {
					argValues.append(String.valueOf(argValue));
					argValues.append(",");
				}
			}
			throw new ReflectionException("Error instantiating " + type + " with invalid types (" + argTypes
					+ ") or values (" + argValues + "). Cause: " + e, e);
		}
	}

	//设置属性
	public void setProperties(Properties properties) {
		//默认没有属性可以设置
	}
	
	//是否是集合类型
	public <T> boolean isCollection(Class<T> type) {
		//是否是Collection的子类
		return Collection.class.isAssignableFrom(type);
	}

}
