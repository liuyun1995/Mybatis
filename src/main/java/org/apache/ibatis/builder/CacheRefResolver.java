package org.apache.ibatis.builder;

import org.apache.ibatis.cache.Cache;

//缓存引用解析器
public class CacheRefResolver {
	
	private final MapperBuilderAssistant assistant;  //mapper构建助手
	private final String cacheRefNamespace;          //缓存关系名称空间

	public CacheRefResolver(MapperBuilderAssistant assistant, String cacheRefNamespace) {
		this.assistant = assistant;
		this.cacheRefNamespace = cacheRefNamespace;
	}

	public Cache resolveCacheRef() {
		return assistant.useCacheRef(cacheRefNamespace);
	}
	
}