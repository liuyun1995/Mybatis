package org.apache.ibatis.parsing;

//记号处理器接口
public interface TokenHandler {
	
	String handleToken(String content);

}
