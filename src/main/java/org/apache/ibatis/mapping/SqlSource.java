package org.apache.ibatis.mapping;

//SQL源码, 表示从XML文件或注释读取的映射语句的内容 它从用户那里接收到输入参数并创建SQL, 然后传送给数据库
public interface SqlSource {

	BoundSql getBoundSql(Object parameterObject);

}
