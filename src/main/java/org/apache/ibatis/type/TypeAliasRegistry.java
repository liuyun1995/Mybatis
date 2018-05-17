package org.apache.ibatis.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.io.Resources;

//类型别名注册器
public class TypeAliasRegistry {

	//类型别名映射表
	private final Map<String, Class<?>> TYPE_ALIASES = new HashMap<String, Class<?>>();

	public TypeAliasRegistry() {
		//构造函数里注册系统内置的类型别名
		registerAlias("string", String.class);

		//基本包装类型
		registerAlias("byte", Byte.class);
		registerAlias("long", Long.class);
		registerAlias("short", Short.class);
		registerAlias("int", Integer.class);
		registerAlias("integer", Integer.class);
		registerAlias("double", Double.class);
		registerAlias("float", Float.class);
		registerAlias("boolean", Boolean.class);

		//基本数组包装类型
		registerAlias("byte[]", Byte[].class);
		registerAlias("long[]", Long[].class);
		registerAlias("short[]", Short[].class);
		registerAlias("int[]", Integer[].class);
		registerAlias("integer[]", Integer[].class);
		registerAlias("double[]", Double[].class);
		registerAlias("float[]", Float[].class);
		registerAlias("boolean[]", Boolean[].class);

		//基本类型
		registerAlias("_byte", byte.class);
		registerAlias("_long", long.class);
		registerAlias("_short", short.class);
		registerAlias("_int", int.class);
		registerAlias("_integer", int.class);
		registerAlias("_double", double.class);
		registerAlias("_float", float.class);
		registerAlias("_boolean", boolean.class);

		//基本数组类型
		registerAlias("_byte[]", byte[].class);
		registerAlias("_long[]", long[].class);
		registerAlias("_short[]", short[].class);
		registerAlias("_int[]", int[].class);
		registerAlias("_integer[]", int[].class);
		registerAlias("_double[]", double[].class);
		registerAlias("_float[]", float[].class);
		registerAlias("_boolean[]", boolean[].class);

		//日期数字型
		registerAlias("date", Date.class);
		registerAlias("decimal", BigDecimal.class);
		registerAlias("bigdecimal", BigDecimal.class);
		registerAlias("biginteger", BigInteger.class);
		registerAlias("object", Object.class);

		registerAlias("date[]", Date[].class);
		registerAlias("decimal[]", BigDecimal[].class);
		registerAlias("bigdecimal[]", BigDecimal[].class);
		registerAlias("biginteger[]", BigInteger[].class);
		registerAlias("object[]", Object[].class);

		//集合类型
		registerAlias("map", Map.class);
		registerAlias("hashmap", HashMap.class);
		registerAlias("list", List.class);
		registerAlias("arraylist", ArrayList.class);
		registerAlias("collection", Collection.class);
		registerAlias("iterator", Iterator.class);

		//ResultSet型
		registerAlias("ResultSet", ResultSet.class);
	}

	//解析类型别名,如果不能分配类型, 则抛出类转换异常
	@SuppressWarnings("unchecked")
	public <T> Class<T> resolveAlias(String string) {
		try {
			if (string == null) {
				return null;
			}
			//转成小写
			String key = string.toLowerCase(Locale.ENGLISH);
			Class<T> value;
			if (TYPE_ALIASES.containsKey(key)) {
				//若在类型别名映射中则直接获取
				value = (Class<T>) TYPE_ALIASES.get(key);
			} else {
				//否则根据字符串尝试加载类
				value = (Class<T>) Resources.classForName(string);
			}
			return value;
		} catch (ClassNotFoundException e) {
			throw new TypeException("Could not resolve type alias '" + string + "'.  Cause: " + e, e);
		}
	}

	//注册类型别名(指定包下)
	public void registerAliases(String packageName) {
		registerAliases(packageName, Object.class);
	}

	//注册类型别名(指定包下和指定父类)
	public void registerAliases(String packageName, Class<?> superType) {
		ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
		//在指定包下寻找所有子类
		resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
		//获取找到的类型集合
		Set<Class<? extends Class<?>>> typeSet = resolverUtil.getClasses();
		//遍历找到的类型集合
		for (Class<?> type : typeSet) {
			//若该类型不是匿名类, 不是接口, 不是成员类, 才进行注册
			if (!type.isAnonymousClass() && !type.isInterface() && !type.isMemberClass()) {
				registerAlias(type);
			}
		}
	}

	//注册类型别名(指定类型)
	public void registerAlias(Class<?> type) {
		//获取简单类名
		String alias = type.getSimpleName();
		//获取该类的Alias注解
		Alias aliasAnnotation = type.getAnnotation(Alias.class);
		//若存在Alias注解, 优先使用注解的别名
		if (aliasAnnotation != null) {
			alias = aliasAnnotation.value();
		}
		//注册别名和类型
		registerAlias(alias, type);
	}

	//注册类型别名
	public void registerAlias(String alias, Class<?> value) {
		if (alias == null) {
			throw new TypeException("The parameter alias cannot be null");
		}
		//转成小写
		String key = alias.toLowerCase(Locale.ENGLISH);
		//若已存在key, 则验证value是否一样, 不一样则报错
		if (TYPE_ALIASES.containsKey(key) && TYPE_ALIASES.get(key) != null && !TYPE_ALIASES.get(key).equals(value)) {
			throw new TypeException("The alias '" + alias + "' is already mapped to the value '"
					+ TYPE_ALIASES.get(key).getName() + "'.");
		}
		//放入类型别名映射表
		TYPE_ALIASES.put(key, value);
	}

	//注册类型别名
	public void registerAlias(String alias, String value) {
		try {
			registerAlias(alias, Resources.classForName(value));
		} catch (ClassNotFoundException e) {
			throw new TypeException("Error registering type alias " + alias + " for " + value + ". Cause: " + e, e);
		}
	}
	
	//获取类型别名映射
	public Map<String, Class<?>> getTypeAliases() {
		return Collections.unmodifiableMap(TYPE_ALIASES);
	}

}
