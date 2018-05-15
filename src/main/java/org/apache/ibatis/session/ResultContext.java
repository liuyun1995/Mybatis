package org.apache.ibatis.session;

/**
 * 结果上下文
 */
public interface ResultContext {

	// 获取结果
	Object getResultObject();

	// 获取记录数
	int getResultCount();

	boolean isStopped();

	// 停止?
	void stop();

}
