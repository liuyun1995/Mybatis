package org.apache.ibatis.io;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

//寻找包下满足条件的所有类
public class ResolverUtil<T> {
	
	private static final Log log = LogFactory.getLog(ResolverUtil.class);
	
	//类型验证接口
	public static interface Test {
		//验证方法
		boolean matches(Class<?> type);
	}
	
	public static class IsA implements Test {
		//父类型
		private Class<?> parent;
		
		public IsA(Class<?> parentType) {
			this.parent = parentType;
		}
		//验证方法
		public boolean matches(Class<?> type) {
			return type != null && parent.isAssignableFrom(type);
		}
		
		@Override
		public String toString() {
			return "is assignable to " + parent.getSimpleName();
		}
		
	}
	
	public static class AnnotatedWith implements Test {
		//注解类型
		private Class<? extends Annotation> annotation;
		
		public AnnotatedWith(Class<? extends Annotation> annotation) {
			this.annotation = annotation;
		}
		//验证方法
		public boolean matches(Class<?> type) {
			return type != null && type.isAnnotationPresent(annotation);
		}
		
		@Override
		public String toString() {
			return "annotated with @" + annotation.getSimpleName();
		}
		
	}
	
	//匹配类的集合
	private Set<Class<? extends T>> matches = new HashSet<Class<? extends T>>();
	
	//类加载器
	private ClassLoader classloader;
	
	//获取匹配类的集合
	public Set<Class<? extends T>> getClasses() {
		return matches;
	}
	
	//获取类加载器
	public ClassLoader getClassLoader() {
		return classloader == null ? Thread.currentThread().getContextClassLoader() : classloader;
	}
	
	//设置类加载器
	public void setClassLoader(ClassLoader classloader) {
		this.classloader = classloader;
	}
	
	//寻找指定类型的实现类
	public ResolverUtil<T> findImplementations(Class<?> parent, String... packageNames) {
		if (packageNames == null) {
			return this;
		}
		//生成类型验证器
		Test test = new IsA(parent);
		//遍历所有包寻找符合条件的类
		for (String pkg : packageNames) {
			find(test, pkg);
		}
		//返回ResolverUtil对象本身
		return this;
	}
	
	//寻找注解类型
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
	
	
	//寻找指定包下满足条件的所有类
	public ResolverUtil<T> find(Test test, String packageName) {
		//根据包名获取路径名
		String path = getPackagePath(packageName);
		try {
			//通过VFS来深入jar包里面去找一个class
			List<String> children = VFS.getInstance().list(path);
			for (String child : children) {
				//如果文件名以".class"结尾
				if (child.endsWith(".class")) {
					//进行类型验证, 然后添加到匹配集合中
					addIfMatching(test, child);
				}
			}
		} catch (IOException ioe) {
			log.error("Could not read package: " + packageName, ioe);
		}
		return this;
	}
	
	//根据包名获取路径名
	protected String getPackagePath(String packageName) {
		return packageName == null ? null : packageName.replace('.', '/');
	}
	
	
	@SuppressWarnings("unchecked")
	protected void addIfMatching(Test test, String fqn) {
		try {
			//获取不带后缀的文件名, 并将"."替换成"/"
			String externalName = fqn.substring(0, fqn.indexOf('.')).replace('/', '.');
			//获取类加载器
			ClassLoader loader = getClassLoader();
			log.debug("Checking to see if class " + externalName + " matches criteria [" + test + "]");
			//用类加载器加载类
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