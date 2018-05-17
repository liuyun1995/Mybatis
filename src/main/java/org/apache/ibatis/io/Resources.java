package org.apache.ibatis.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Properties;

public class Resources {

	//类加载器包装器
	private static ClassLoaderWrapper classLoaderWrapper = new ClassLoaderWrapper();
	//文件编码
	private static Charset charset;

	Resources() {}
	
	//获取默认类加载器
	public static ClassLoader getDefaultClassLoader() {
		return classLoaderWrapper.defaultClassLoader;
	}
	
	//设置默认类加载器
	public static void setDefaultClassLoader(ClassLoader defaultClassLoader) {
		classLoaderWrapper.defaultClassLoader = defaultClassLoader;
	}
	
	//通过Resource获取URL
	public static URL getResourceURL(String resource) throws IOException {
		return getResourceURL(null, resource);
	}
	
	//通过Resource获取URL
	public static URL getResourceURL(ClassLoader loader, String resource) throws IOException {
		URL url = classLoaderWrapper.getResourceAsURL(resource, loader);
		if (url == null) {
			throw new IOException("Could not find resource " + resource);
		}
		return url;
	}
	
	//通过Resource获取InputStream
	public static InputStream getResourceAsStream(String resource) throws IOException {
		return getResourceAsStream(null, resource);
	}
	
	//通过Resource获取InputStream
	public static InputStream getResourceAsStream(ClassLoader loader, String resource) throws IOException {
		InputStream in = classLoaderWrapper.getResourceAsStream(resource, loader);
		if (in == null) {
			throw new IOException("Could not find resource " + resource);
		}
		return in;
	}
	
	//通过Resource获取Properties
	public static Properties getResourceAsProperties(String resource) throws IOException {
		Properties props = new Properties();
		InputStream in = getResourceAsStream(resource);
		props.load(in);
		in.close();
		return props;
	}
	
	//通过Resource获取Properties
	public static Properties getResourceAsProperties(ClassLoader loader, String resource) throws IOException {
		Properties props = new Properties();
		InputStream in = getResourceAsStream(loader, resource);
		props.load(in);
		in.close();
		return props;
	}
	
	//通过Resource获取Reader
	public static Reader getResourceAsReader(String resource) throws IOException {
		Reader reader;
		if (charset == null) {
			reader = new InputStreamReader(getResourceAsStream(resource));
		} else {
			reader = new InputStreamReader(getResourceAsStream(resource), charset);
		}
		return reader;
	}
	
	//通过Resource获取Reader
	public static Reader getResourceAsReader(ClassLoader loader, String resource) throws IOException {
		Reader reader;
		if (charset == null) {
			reader = new InputStreamReader(getResourceAsStream(loader, resource));
		} else {
			reader = new InputStreamReader(getResourceAsStream(loader, resource), charset);
		}
		return reader;
	}
	
	//通过Resource获取File
	public static File getResourceAsFile(String resource) throws IOException {
		return new File(getResourceURL(resource).getFile());
	}
	
	//通过Resource获取File
	public static File getResourceAsFile(ClassLoader loader, String resource) throws IOException {
		return new File(getResourceURL(loader, resource).getFile());
	}
	
	//通过url获取InputStream
	public static InputStream getUrlAsStream(String urlString) throws IOException {
		URL url = new URL(urlString);
		URLConnection conn = url.openConnection();
		return conn.getInputStream();
	}
	
	//通过url获取Reader
	public static Reader getUrlAsReader(String urlString) throws IOException {
		Reader reader;
		if (charset == null) {
			reader = new InputStreamReader(getUrlAsStream(urlString));
		} else {
			reader = new InputStreamReader(getUrlAsStream(urlString), charset);
		}
		return reader;
	}
	
	//通过url获取Properties
	public static Properties getUrlAsProperties(String urlString) throws IOException {
		Properties props = new Properties();
		InputStream in = getUrlAsStream(urlString);
		props.load(in);
		in.close();
		return props;
	}
	
	//根据类名获取Class
	public static Class<?> classForName(String className) throws ClassNotFoundException {
		return classLoaderWrapper.classForName(className);
	}

	//获取编码
	public static Charset getCharset() {
		return charset;
	}

	//设置编码
	public static void setCharset(Charset charset) {
		Resources.charset = charset;
	}

}
