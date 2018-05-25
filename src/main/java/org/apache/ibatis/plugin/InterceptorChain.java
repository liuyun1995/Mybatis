package org.apache.ibatis.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//拦截器链
public class InterceptorChain {

	//拦截器列表
	private final List<Interceptor> interceptors = new ArrayList<Interceptor>();

	//对目标执行所有插件方法
	public Object pluginAll(Object target) {
		//遍历所有拦截器, 执行拦截器的plugin方法
		for (Interceptor interceptor : interceptors) {
			target = interceptor.plugin(target);
		}
		return target;
	}

	//添加拦截器
	public void addInterceptor(Interceptor interceptor) {
		interceptors.add(interceptor);
	}

	//获取所有拦截器
	public List<Interceptor> getInterceptors() {
		return Collections.unmodifiableList(interceptors);
	}

}
