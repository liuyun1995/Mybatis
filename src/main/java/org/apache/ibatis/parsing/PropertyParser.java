package org.apache.ibatis.parsing;

import java.util.Properties;

//属性解析器
public class PropertyParser {

	private PropertyParser() {}

	public static String parse(String string, Properties variables) {
		//新建变量记号处理器
		VariableTokenHandler handler = new VariableTokenHandler(variables);
		//新建通用记号解析器
		GenericTokenParser parser = new GenericTokenParser("${", "}", handler);
		return parser.parse(string);
	}
	
	//变量记号处理器
	private static class VariableTokenHandler implements TokenHandler {
		private Properties variables;
		public VariableTokenHandler(Properties variables) {
			this.variables = variables;
		}
		public String handleToken(String content) {
			//如果属性集合包含了content, 则返回属性值
			if (variables != null && variables.containsKey(content)) {
				return variables.getProperty(content);
			}
			//否则包装回${}形式
			return "${" + content + "}";
		}
	}
	
}
