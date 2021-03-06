package org.apache.ibatis.mapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

//绑定的SQL语句
public class BoundSql {

	private String sql;                                  //SQL语句
	private List<ParameterMapping> parameterMappings;    //参数映射集合
	private Object parameterObject;                      //参数对象
	private Map<String, Object> additionalParameters;    //额外的参数映射
	private MetaObject metaParameters;                   //元对象

	public BoundSql(Configuration configuration, String sql, List<ParameterMapping> parameterMappings,
			Object parameterObject) {
		this.sql = sql;
		this.parameterMappings = parameterMappings;
		this.parameterObject = parameterObject;
		this.additionalParameters = new HashMap<String, Object>();
		this.metaParameters = configuration.newMetaObject(additionalParameters);
	}

	//获取原生sql语句
	public String getSql() {
		return sql;
	}

	//获取参数映射列表
	public List<ParameterMapping> getParameterMappings() {
		return parameterMappings;
	}

	public Object getParameterObject() {
		return parameterObject;
	}

	public boolean hasAdditionalParameter(String name) {
		return metaParameters.hasGetter(name);
	}

	public void setAdditionalParameter(String name, Object value) {
		metaParameters.setValue(name, value);
	}

	public Object getAdditionalParameter(String name) {
		return metaParameters.getValue(name);
	}
	
}
