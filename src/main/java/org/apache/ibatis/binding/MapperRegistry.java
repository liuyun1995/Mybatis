package org.apache.ibatis.binding;

import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//Mapper注册器
public class MapperRegistry {

	//配置信息
	private Configuration config;
	//已知Mapper映射
	private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<Class<?>, MapperProxyFactory<?>>();

	//构造器
	public MapperRegistry(Configuration config) {
		this.config = config;
	}

	//获取Mapper代理类
	@SuppressWarnings("unchecked")
	public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
		//获取mapper代理工厂
		final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
		if (mapperProxyFactory == null) {
			throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
		}
		try {
			//通过代理工厂生成mapper代理类
			return mapperProxyFactory.newInstance(sqlSession);
		} catch (Exception e) {
			throw new BindingException("Error getting mapper instance. Cause: " + e, e);
		}
	}

	//判断是否存在该Mapper
	public <T> boolean hasMapper(Class<T> type) {
		return knownMappers.containsKey(type);
	}

	//添加Mapper方法
	public <T> void addMapper(Class<T> type) {
		//验证该类型是否是接口
		if (type.isInterface()) {
			//若已存在该Mapper则抛出异常
			if (hasMapper(type)) {
				throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
			}
			boolean loadCompleted = false;
			try {
				//将类型和代理工厂放入映射表中
				knownMappers.put(type, new MapperProxyFactory<T>(type));
				MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
				parser.parse();
				loadCompleted = true;
			} finally {
				//如果出现异常则将其移除
				if (!loadCompleted) {
					knownMappers.remove(type);
				}
			}
		}
	}
	
	//获取所有Mapper集合
	public Collection<Class<?>> getMappers() {
		return Collections.unmodifiableCollection(knownMappers.keySet());
	}
	
	//添加所有的Mapper(指定包下和指定父类)
	public void addMappers(String packageName, Class<?> superType) {
		ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
		resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
		Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
		for (Class<?> mapperClass : mapperSet) {
			addMapper(mapperClass);
		}
	}

	//添加所有的Mapper(指定包下)
	public void addMappers(String packageName) {
		addMappers(packageName, Object.class);
	}

}
