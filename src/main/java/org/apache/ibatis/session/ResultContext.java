package org.apache.ibatis.session;

//结果上下文
public interface ResultContext {
	
	Object getResultObject();
	
	int getResultCount();

	boolean isStopped();
	
	void stop();

}
