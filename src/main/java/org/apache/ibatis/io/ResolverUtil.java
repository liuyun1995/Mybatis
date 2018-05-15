package org.apache.ibatis.io;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * 找一个package下满足条件的所有类
 */
public class ResolverUtil<T> {
	
	private static final Log log = LogFactory.getLog(ResolverUtil.class);
	
	public static interface Test {
		boolean matches(Class<?> type);
	}
	
	public static class IsA implements Test {
		private Class<?> parent;
		public IsA(Class<?> parentType) {
			this.parent = parentType;
		}
		public boolean matches(Class<?> type) {
			return type != null && parent.isAssignableFrom(type);
		}
		@Override
		public String toString() {
			return "is assignable to " + parent.getSimpleName();
		}
	}
	
	public static class AnnotatedWith implements Test {
		private Class<? extends Annotation> annotation;
		public AnnotatedWith(Class<? extends Annotation> annotation) {
			this.annotation = annotation;
		}
		public boolean matches(Class<?> type) {
			return type != null && type.isAnnotationPresent(annotation);
		}
		@Override
		public String toString() {
			return "annotated with @" + annotation.getSimpleName();
		}
	}
	
	private Set<Class<? extends T>> matches = new HashSet<Class<? extends T>>();
	
	private ClassLoader classloader;
	
	public Set<Class<? extends T>> getClasses() {
		return matches;
	}
	
	public ClassLoader getClassLoader() {
		return classloader == null ? Thread.currentThread().getContextClassLoader() : classloader;
	}
	
	public void setClassLoader(ClassLoader classloader) {
		this.classloader = classloader;
	}
	
	public ResolverUtil<T> findImplementations(Class<?> parent, String... packageNames) {
		if (packageNames == null) {
			return this;
		}
		Test test = new IsA(parent);
		for (String pkg : packageNames) {
			find(test, pkg);
		}
		return this;
	}
	
	
	public ResolverUtil<T> findAnnotated(Class<? extends Annotation> annotation, String... packageNames) {
		if (packageNames == null) {
			return this;
		}
		Test test = new AnnotatedWith(annotation);
		for (String pkg : packageNames) {
			find(test, pkg);
		}
		return this;
	}
	
	
	// 主要的方法，找一个package下满足条件的所有类,被TypeHanderRegistry,MapperRegistry,TypeAliasRegistry调用
	public ResolverUtil<T> find(Test test, String packageName) {
		String path = getPackagePath(packageName);
		try {
			// 通过VFS来深入jar包里面去找一个class
			List<String> children = VFS.getInstance().list(path);
			for (String child : children) {
				if (child.endsWith(".class")) {
					addIfMatching(test, child);
				}
			}
		} catch (IOException ioe) {
			log.error("Could not read package: " + packageName, ioe);
		}
		return this;
	}
	
	
	protected String getPackagePath(String packageName) {
		return packageName == null ? null : packageName.replace('.', '/');
	}
	
	
	@SuppressWarnings("unchecked")
	protected void addIfMatching(Test test, String fqn) {
		try {
			String externalName = fqn.substring(0, fqn.indexOf('.')).replace('/', '.');
			ClassLoader loader = getClassLoader();
			log.debug("Checking to see if class " + externalName + " matches criteria [" + test + "]");
			Class<?> type = loader.loadClass(externalName);
			if (test.matches(type)) {
				matches.add((Class<T>) type);
			}
		} catch (Throwable t) {
			log.warn("Could not examine class '" + fqn + "'" + " due to a " + t.getClass().getName() + " with message: "
					+ t.getMessage());
		}
	}
}