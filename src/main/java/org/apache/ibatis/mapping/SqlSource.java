package org.apache.ibatis.mapping;

//SQL源码
public interface SqlSource {

	BoundSql getBoundSql(Object parameterObject);

}
