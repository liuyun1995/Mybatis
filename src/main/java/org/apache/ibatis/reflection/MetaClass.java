package org.apache.ibatis.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

//原类型
public class MetaClass {
	
	//反射器
	private Reflector reflector;

	//构造器
	private MetaClass(Class<?> type) {
		this.reflector = Reflector.forClass(type);
	}

	//根据类型获取原类型
	public static MetaClass forClass(Class<?> type) {
		return new MetaClass(type);
	}

	public static boolean isClassCacheEnabled() {
		return Reflector.isClassCacheEnabled();
	}

	public static void setClassCacheEnabled(boolean classCacheEnabled) {
		Reflector.setClassCacheEnabled(classCacheEnabled);
	}

	public MetaClass metaClassForProperty(String name) {
		Class<?> propType = reflector.getGetterType(name);
		return MetaClass.forClass(propType);
	}

	public String findProperty(String name) {
		StringBuilder prop = buildProperty(name, new StringBuilder());
		return prop.length() > 0 ? prop.toString() : null;
	}

	public String findProperty(String name, boolean useCamelCaseMapping) {
		if (useCamelCaseMapping) {
			name = name.replace("_", "");
		}
		return findProperty(name);
	}

	//获取getter方法名称集合
	public String[] getGetterNames() {
		return reflector.getGetablePropertyNames();
	}

	//获取setter方法名称集合
	public String[] getSetterNames() {
		return reflector.getSetablePropertyNames();
	}

	public Class<?> getSetterType(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			MetaClass metaProp = metaClassForProperty(prop.getName());
			return metaProp.getSetterType(prop.getChildren());
		} else {
			return reflector.getSetterType(prop.getName());
		}
	}

	public Class<?> getGetterType(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			MetaClass metaProp = metaClassForProperty(prop);
			return metaProp.getGetterType(prop.getChildren());
		}
		return getGetterType(prop);
	}

	private MetaClass metaClassForProperty(PropertyTokenizer prop) {
		Class<?> propType = getGetterType(prop);
		return MetaClass.forClass(propType);
	}

	private Class<?> getGetterType(PropertyTokenizer prop) {
		Class<?> type = reflector.getGetterType(prop.getName());
		if (prop.getIndex() != null && Collection.class.isAssignableFrom(type)) {
			Type returnType = getGenericGetterType(prop.getName());
			if (returnType instanceof ParameterizedType) {
				Type[] actualTypeArguments = ((ParameterizedType) returnType).getActualTypeArguments();
				if (actualTypeArguments != null && actualTypeArguments.length == 1) {
					returnType = actualTypeArguments[0];
					if (returnType instanceof Class) {
						type = (Class<?>) returnType;
					} else if (returnType instanceof ParameterizedType) {
						type = (Class<?>) ((ParameterizedType) returnType).getRawType();
					}
				}
			}
		}
		return type;
	}

	private Type getGenericGetterType(String propertyName) {
		try {
			Invoker invoker = reflector.getGetInvoker(propertyName);
			if (invoker instanceof MethodInvoker) {
				Field _method = MethodInvoker.class.getDeclaredField("method");
				_method.setAccessible(true);
				Method method = (Method) _method.get(invoker);
				return method.getGenericReturnType();
			} else if (invoker instanceof GetFieldInvoker) {
				Field _field = GetFieldInvoker.class.getDeclaredField("field");
				_field.setAccessible(true);
				Field field = (Field) _field.get(invoker);
				return field.getGenericType();
			}
		} catch (NoSuchFieldException e) {
		} catch (IllegalAccessException e) {
		}
		return null;
	}

	//是否有setter方法
	public boolean hasSetter(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			if (reflector.hasSetter(prop.getName())) {
				MetaClass metaProp = metaClassForProperty(prop.getName());
				return metaProp.hasSetter(prop.getChildren());
			} else {
				return false;
			}
		} else {
			return reflector.hasSetter(prop.getName());
		}
	}

	//是否有getter方法
	public boolean hasGetter(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			if (reflector.hasGetter(prop.getName())) {
				MetaClass metaProp = metaClassForProperty(prop);
				return metaProp.hasGetter(prop.getChildren());
			} else {
				return false;
			}
		} else {
			return reflector.hasGetter(prop.getName());
		}
	}

	public Invoker getGetInvoker(String name) {
		return reflector.getGetInvoker(name);
	}

	public Invoker getSetInvoker(String name) {
		return reflector.getSetInvoker(name);
	}

	private StringBuilder buildProperty(String name, StringBuilder builder) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			String propertyName = reflector.findPropertyName(prop.getName());
			if (propertyName != null) {
				builder.append(propertyName);
				builder.append(".");
				MetaClass metaProp = metaClassForProperty(propertyName);
				metaProp.buildProperty(prop.getChildren(), builder);
			}
		} else {
			String propertyName = reflector.findPropertyName(name);
			if (propertyName != null) {
				builder.append(propertyName);
			}
		}
		return builder;
	}

	public boolean hasDefaultConstructor() {
		return reflector.hasDefaultConstructor();
	}

}
