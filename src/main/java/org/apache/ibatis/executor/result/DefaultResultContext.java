package org.apache.ibatis.executor.result;

import org.apache.ibatis.session.ResultContext;

//默认结果上下文
public class DefaultResultContext implements ResultContext {

	private Object resultObject;
	private int resultCount;
	private boolean stopped;

	public DefaultResultContext() {
		resultObject = null;
		resultCount = 0;
		stopped = false;
	}

	public Object getResultObject() {
		return resultObject;
	}

	public int getResultCount() {
		return resultCount;
	}

	public boolean isStopped() {
		return stopped;
	}

	//应该是每次调用nextResultObject这个方法，这样内部count就加1
	public void nextResultObject(Object resultObject) {
		resultCount++;
		this.resultObject = resultObject;
	}

	public void stop() {
		this.stopped = true;
	}

}
