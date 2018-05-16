package org.apache.ibatis.reflection.property;

import java.lang.reflect.Field;

//Bean属性复制器
public final class PropertyCopier {

	private PropertyCopier() {}

	//复制Bean之间的属性
	public static void copyBeanProperties(Class<?> type, Object sourceBean, Object destinationBean) {
		Class<?> parent = type;
		while (parent != null) {
			//循环将父类的属性都要复制过来
			final Field[] fields = parent.getDeclaredFields();
			for (Field field : fields) {
				try {
					field.setAccessible(true);
					field.set(destinationBean, field.get(sourceBean));
				} catch (Exception e) {
					// Nothing useful to do, will only fail on final fields, which will be ignored.
				}
			}
			parent = parent.getSuperclass();
		}
	}

}
