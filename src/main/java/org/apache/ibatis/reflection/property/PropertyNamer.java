package org.apache.ibatis.reflection.property;

import java.util.Locale;

import org.apache.ibatis.reflection.ReflectionException;

//属性命名器
public final class PropertyNamer {

	private PropertyNamer() {}

	//方法转为属性
	public static String methodToProperty(String name) {
		//去掉get|set|is
		if (name.startsWith("is")) {
			name = name.substring(2);
		} else if (name.startsWith("get") || name.startsWith("set")) {
			name = name.substring(3);
		} else {
			throw new ReflectionException(
					"Error parsing property name '" + name + "'.  Didn't start with 'is', 'get' or 'set'.");
		}
		// 如果只有1个字母-->转为小写
		// 如果大于1个字母,第二个字母非大写-->转为小写
		// String uRL --> String getuRL() {
		if (name.length() == 1 || (name.length() > 1 && !Character.isUpperCase(name.charAt(1)))) {
			name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
		}
		return name;
	}

	//是否是属性
	public static boolean isProperty(String name) {
		//必须以get|set|is开头
		return name.startsWith("get") || name.startsWith("set") || name.startsWith("is");
	}

	//是否是getter
	public static boolean isGetter(String name) {
		return name.startsWith("get") || name.startsWith("is");
	}

	//是否是setter
	public static boolean isSetter(String name) {
		return name.startsWith("set");
	}

}
