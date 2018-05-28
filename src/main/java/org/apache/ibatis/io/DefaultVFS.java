package org.apache.ibatis.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

//默认的VFS, 提供了读取jar包的方法
public class DefaultVFS extends VFS {
	
	private static final Log log = LogFactory.getLog(ResolverUtil.class);
	
	private static final byte[] JAR_MAGIC = { 'P', 'K', 3, 4 };
	
	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public List<String> list(URL url, String path) throws IOException {
		InputStream is = null;
		try {
			List<String> resources = new ArrayList<String>();
			URL jarUrl = findJarForResource(url);
			if (jarUrl != null) {
				is = jarUrl.openStream();
				log.debug("Listing " + url);
				// 用JDK自带的JarInputStream来读取jar包
				resources = listResources(new JarInputStream(is), path);
			} else {
				List<String> children = new ArrayList<String>();
				try {
					if (isJar(url)) {
						is = url.openStream();
						JarInputStream jarInput = new JarInputStream(is);
						log.debug("Listing " + url);
						for (JarEntry entry; (entry = jarInput.getNextJarEntry()) != null;) {
							log.debug("Jar entry: " + entry.getName());
							children.add(entry.getName());
						}
						jarInput.close();
					} else {
						is = url.openStream();
						BufferedReader reader = new BufferedReader(new InputStreamReader(is));
						List<String> lines = new ArrayList<String>();
						for (String line; (line = reader.readLine()) != null;) {
							log.debug("Reader entry: " + line);
							lines.add(line);
							if (getResources(path + "/" + line).isEmpty()) {
								lines.clear();
								break;
							}
						}
						if (!lines.isEmpty()) {
							log.debug("Listing " + url);
							children.addAll(lines);
						}
					}
				} catch (FileNotFoundException e) {
					/*
					 * For file URLs the openStream() call might fail, depending on the servlet
					 * container, because directories can't be opened for reading. If that happens,
					 * then list the directory directly instead.
					 */
					if ("file".equals(url.getProtocol())) {
						File file = new File(url.getFile());
						log.debug("Listing directory " + file.getAbsolutePath());
						if (file.isDirectory()) {
							log.debug("Listing " + url);
							children = Arrays.asList(file.list());
						}
					} else {
						// No idea where the exception came from so rethrow it
						throw e;
					}
				}
				// The URL prefix to use when recursively listing child resources
				String prefix = url.toExternalForm();
				if (!prefix.endsWith("/")) {
					prefix = prefix + "/";
				}
				// Iterate over immediate children, adding files and recursing into directories
				for (String child : children) {
					String resourcePath = path + "/" + child;
					resources.add(resourcePath);
					URL childUrl = new URL(prefix + child);
					resources.addAll(list(childUrl, resourcePath));
				}
			}
			return resources;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {
					// Ignore
				}
			}
		}
	}

	//获取资源列表
	protected List<String> listResources(JarInputStream jar, String path) throws IOException {
		// Include the leading and trailing slash when matching names
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		if (!path.endsWith("/")) {
			path = path + "/";
		}

		// Iterate over the entries and collect those that begin with the requested path
		List<String> resources = new ArrayList<String>();
		for (JarEntry entry; (entry = jar.getNextJarEntry()) != null;) {
			if (!entry.isDirectory()) {
				// Add leading slash if it's missing
				String name = entry.getName();
				if (!name.startsWith("/")) {
					name = "/" + name;
				}

				// Check file name
				if (name.startsWith(path)) {
					log.debug("Found resource: " + name);
					// Trim leading slash
					resources.add(name.substring(1));
				}
			}
		}
		return resources;
	}

	//获取指定资源
	protected URL findJarForResource(URL url) throws MalformedURLException {
		log.debug("Find JAR URL: " + url);

		// If the file part of the URL is itself a URL, then that URL probably points to
		// the JAR
		try {
			for (;;) {
				url = new URL(url.getFile());
				log.debug("Inner URL: " + url);
			}
		} catch (MalformedURLException e) {
			// This will happen at some point and serves as a break in the loop
		}

		// Look for the .jar extension and chop off everything after that
		StringBuilder jarUrl = new StringBuilder(url.toExternalForm());
		int index = jarUrl.lastIndexOf(".jar");
		if (index >= 0) {
			jarUrl.setLength(index + 4);
			log.debug("Extracted JAR URL: " + jarUrl);
		} else {
			log.debug("Not a JAR: " + jarUrl);
			return null;
		}

		// Try to open and test it
		try {
			URL testUrl = new URL(jarUrl.toString());
			if (isJar(testUrl)) {
				return testUrl;
			} else {
				// WebLogic fix: check if the URL's file exists in the filesystem.
				log.debug("Not a JAR: " + jarUrl);
				jarUrl.replace(0, jarUrl.length(), testUrl.getFile());
				File file = new File(jarUrl.toString());

				// File name might be URL-encoded
				if (!file.exists()) {
					try {
						file = new File(URLEncoder.encode(jarUrl.toString(), "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException("Unsupported encoding?  UTF-8?  That's unpossible.");
					}
				}

				if (file.exists()) {
					log.debug("Trying real file: " + file.getAbsolutePath());
					testUrl = file.toURI().toURL();
					if (isJar(testUrl)) {
						return testUrl;
					}
				}
			}
		} catch (MalformedURLException e) {
			log.warn("Invalid JAR URL: " + jarUrl);
		}
		log.debug("Not a JAR: " + jarUrl);
		return null;
	}
	
	protected String getPackagePath(String packageName) {
		return packageName == null ? null : packageName.replace('.', '/');
	}
	
	protected boolean isJar(URL url) {
		return isJar(url, new byte[JAR_MAGIC.length]);
	}
	
	protected boolean isJar(URL url, byte[] buffer) {
		InputStream is = null;
		try {
			is = url.openStream();
			is.read(buffer, 0, JAR_MAGIC.length);
			if (Arrays.equals(buffer, JAR_MAGIC)) {
				log.debug("Found JAR: " + url);
				return true;
			}
		} catch (Exception e) {
			// Failure to read the stream means this is not a JAR
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {
					// Ignore
				}
			}
		}
		return false;
	}
}
