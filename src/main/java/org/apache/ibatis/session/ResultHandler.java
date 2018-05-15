package org.apache.ibatis.session;

/**
 * 结果处理器
 */
public interface ResultHandler {

	// 处理结果，给一个结果上下文
	void handleResult(ResultContext context);

}
