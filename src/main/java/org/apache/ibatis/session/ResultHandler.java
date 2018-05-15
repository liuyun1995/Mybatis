package org.apache.ibatis.session;

//结果处理器
public interface ResultHandler {
	
	void handleResult(ResultContext context);

}
