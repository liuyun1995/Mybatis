package org.apache.ibatis.io;

import java.io.InputStream;
import java.net.URL;

//类加载器包装类
public class ClassLoaderWrapper {
	
	ClassLoader defaultClassLoader;   //默认类加载器
	ClassLoader systemClassLoader;    //系统类加载器

	ClassLoaderWrapper() {
		try {
			//在构造时初始化系统类加载器
			systemClassLoader = ClassLoader.getSystemClassLoader();
		} catch (SecurityException ignored) {
			//ignore
		}
	}
	
	//通过路径获取URL
	public URL getResourceAsURL(String resource) {
		return getResourceAsURL(resource, getClassLoaders(null));
	}
	
	//通过路径获取URL
	public URL getResourceAsURL(String resource, ClassLoader classLoader) {
		return getResourceAsURL(resource, getClassLoaders(classLoader));
	}
	
	//通过路径获取InputStream
	public InputStream getResourceAsStream(String resource) {
		return getResourceAsStream(resource, getClassLoaders(null));
	}
	
	//通过路径获取InputStream
	public InputStream getResourceAsStream(String resource, ClassLoader classLoader) {
		return getResourceAsStream(resource, getClassLoaders(classLoader));
	}
	
	//通过类名获取Class
	public Class<?> classForName(String name) throws ClassNotFoundException {
		return classForName(name, getClassLoaders(null));
	}
	
	//通过类名获取Class
	public Class<?> classForName(String name, ClassLoader classLoader) throws ClassNotFoundException {
		return classForName(name, getClassLoaders(classLoader));
	}
	
	//通过路径获取InputStream
	InputStream getResourceAsStream(String resource, ClassLoader[] classLoader) {
		for (ClassLoader cl : classLoader) {
			if (null != cl) {
				InputStream returnValue = cl.getResourceAsStream(resource);
				if (null == returnValue) {
					returnValue = cl.getResourceAsStream("/" + resource);
				}
				if (null != returnValue) {
					return returnValue;
				}
			}
		}
		return null;
	}
	
	//通过路径获取URL
	URL getResourceAsURL(String resource, ClassLoader[] classLoader) {
		URL url;
		for (ClassLoader cl : classLoader) {
			if (null != cl) {
				url = cl.getResource(resource);
				if (null == url) {
					url = cl.getResource("/" + resource);
				}
				if (null != url) {
					return url;
				}
			}
		}
		return null;
	}
	
	//通过类名获取Class
	Class<?> classForName(String name, ClassLoader[] classLoader) throws ClassNotFoundException {
		for (ClassLoader cl : classLoader) {
			if (null != cl) {
				try {
					//使用指定类加载器加载类
					Class<?> c = Class.forName(name, true, cl);
					if (null != c) {
						return c;
					}
				} catch (ClassNotFoundException e) {
					//ignore
				}
			}
		}
		throw new ClassNotFoundException("Cannot find class: " + name);
	}
	
	//获取类加载器数组
	ClassLoader[] getClassLoaders(ClassLoader classLoader) {
		return new ClassLoader[] { classLoader,
				                    //默认类加载器
				                    defaultClassLoader,
				                    //线程上下文类加载器
				                    Thread.currentThread().getContextClassLoader(),
				                    getClass().getClassLoader(),
				                    //系统类加载器
				                    systemClassLoader };
	}

}
