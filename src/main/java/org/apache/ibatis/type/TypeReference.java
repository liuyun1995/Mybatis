package org.apache.ibatis.type;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 3.1新加的类型引用,为了引用一个泛型类型
 */
public abstract class TypeReference<T> {

	// 引用的原生类型
	private final Type rawType;

	protected TypeReference() {
		rawType = getSuperclassTypeParameter(getClass());
	}

	Type getSuperclassTypeParameter(Class<?> clazz) {
		// 得到泛型T的实际类型
		Type genericSuperclass = clazz.getGenericSuperclass();
		// ???? 怎么会是Class型
		if (genericSuperclass instanceof Class) {
			// try to climb up the hierarchy until meet something useful
			if (TypeReference.class != genericSuperclass) {
				return getSuperclassTypeParameter(clazz.getSuperclass());
			}

			throw new TypeException("'" + getClass() + "' extends TypeReference but misses the type parameter. "
					+ "Remove the extension or add a type parameter to it.");
		}

		Type rawType = ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0];
		// TODO remove this when Reflector is fixed to return Types
		if (rawType instanceof ParameterizedType) {
			rawType = ((ParameterizedType) rawType).getRawType();
		}

		return rawType;
	}

	public final Type getRawType() {
		return rawType;
	}

	@Override
	public String toString() {
		return rawType.toString();
	}

}
