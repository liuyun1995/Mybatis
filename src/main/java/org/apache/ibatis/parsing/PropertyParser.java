package org.apache.ibatis.parsing;

import java.util.Properties;

//属性解析器
public class PropertyParser {

	private PropertyParser() {}

	public static String parse(String string, Properties variables) {
		VariableTokenHandler handler = new VariableTokenHandler(variables);
		GenericTokenParser parser = new GenericTokenParser("${", "}", handler);
		return parser.parse(string);
	}
	
	private static class VariableTokenHandler implements TokenHandler {
		private Properties variables;
		public VariableTokenHandler(Properties variables) {
			this.variables = variables;
		}
		public String handleToken(String content) {
			if (variables != null && variables.containsKey(content)) {
				return variables.getProperty(content);
			}
			return "${" + content + "}";
		}
	}
	
}
