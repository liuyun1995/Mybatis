package org.apache.ibatis.builder;

import java.util.List;

import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;

//ResultMap解析器
public class ResultMapResolver {
	
	private final MapperBuilderAssistant assistant;   //构建助手
	private String id;                                //结果集ID
	private Class<?> type;                            //结果类型
	private String extend;                            //父结果集
	private Discriminator discriminator;              //鉴别器
	private List<ResultMapping> resultMappings;       //结果映射集合
	private Boolean autoMapping;                      //是否自动映射

	//构造器
	public ResultMapResolver(MapperBuilderAssistant assistant, String id, Class<?> type, String extend,
			Discriminator discriminator, List<ResultMapping> resultMappings, Boolean autoMapping) {
		this.assistant = assistant;
		this.id = id;
		this.type = type;
		this.extend = extend;
		this.discriminator = discriminator;
		this.resultMappings = resultMappings;
		this.autoMapping = autoMapping;
	}

	//生成ResultMap对象
	public ResultMap resolve() {
		//反调构建助手的方法
		return assistant.addResultMap(this.id, this.type, this.extend, this.discriminator, this.resultMappings, this.autoMapping);
	}

}