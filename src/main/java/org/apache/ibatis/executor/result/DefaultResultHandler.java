package org.apache.ibatis.executor.result;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;

/**
 * 默认结果处理器
 */
public class DefaultResultHandler implements ResultHandler {

	// 内部实现是存了一个List
	private final List<Object> list;

	public DefaultResultHandler() {
		list = new ArrayList<Object>();
	}

	// 但不一定是ArrayList,也可以通过ObjectFactory来产生特定的List
	@SuppressWarnings("unchecked")
	public DefaultResultHandler(ObjectFactory objectFactory) {
		list = objectFactory.create(List.class);
	}

	public void handleResult(ResultContext context) {
		// 处理很简单，就是把记录加入List
		list.add(context.getResultObject());
	}

	public List<Object> getResultList() {
		return list;
	}

}
