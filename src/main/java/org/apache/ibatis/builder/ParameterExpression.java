package org.apache.ibatis.builder;

import java.util.HashMap;

//参数表达式
public class ParameterExpression extends HashMap<String, String> {

	private static final long serialVersionUID = -2417552199605158680L;

	public ParameterExpression(String expression) {
		parse(expression);
	}

	//解析表达式方法
	private void parse(String expression) {
		//例子：#{property,javaType=int,jdbcType=NUMERIC}
		//去除空白并返回首个非空白字符的位置
		int p = skipWS(expression, 0);
		if (expression.charAt(p) == '(') {
			//处理表达式
			expression(expression, p + 1);
		} else {
			//处理属性
			property(expression, p);
		}
	}

	// 表达式可能是3.2的新功能，可以先不管
	private void expression(String expression, int left) {
		int match = 1;
		int right = left + 1;
		while (match > 0) {
			if (expression.charAt(right) == ')') {
				match--;
			} else if (expression.charAt(right) == '(') {
				match++;
			}
			right++;
		}
		put("expression", expression.substring(left, right - 1));
		jdbcTypeOpt(expression, right);
	}

	private void property(String expression, int left) {
		// 例子：#{property,javaType=int,jdbcType=NUMERIC}
		// property:VARCHAR
		if (left < expression.length()) {
			// 首先，得到逗号或者冒号之前的字符串，加入到property
			int right = skipUntil(expression, left, ",:");
			// 放置参数名, 在这里的参数名为：property, 所以map是这样的{key=property, value=property}
			put("property", trimmedStr(expression, left, right));
			// 第二，处理javaType，jdbcType
			jdbcTypeOpt(expression, right);
		}
	}

	//去除空格
	private int skipWS(String expression, int p) {
		for (int i = p; i < expression.length(); i++) {
			//0x20表示空格
			if (expression.charAt(i) > 0x20) {
				return i;
			}
		}
		return expression.length();
	}

	private int skipUntil(String expression, int p, final String endChars) {
		// 从位置p开始, 一个一个字符进行遍历
		for (int i = p; i < expression.length(); i++) {
			// 取到表达式的第i个字符
			char c = expression.charAt(i);
			// 如果这个字符在endChars中, 则返回该字符在表达式中的位置
			if (endChars.indexOf(c) > -1) {
				return i;
			}
		}
		return expression.length();
	}

	private void jdbcTypeOpt(String expression, int p) {
		// 例子：#{property,javaType=int,jdbcType=NUMERIC}
		// property:VARCHAR
		// 首先去除空白,返回的p是第一个不是空白的字符位置
		p = skipWS(expression, p);
		if (p < expression.length()) {
			// 第一个property解析完有两种情况，逗号和冒号
			if (expression.charAt(p) == ':') {
				jdbcType(expression, p + 1);
			} else if (expression.charAt(p) == ',') {
				option(expression, p + 1);
			} else {
				throw new BuilderException("Parsing error in {" + new String(expression) + "} in position " + p);
			}
		}
	}

	private void jdbcType(String expression, int p) {
		// property:VARCHAR
		int left = skipWS(expression, p);
		int right = skipUntil(expression, left, ",");
		if (right > left) {
			put("jdbcType", trimmedStr(expression, left, right));
		} else {
			throw new BuilderException("Parsing error in {" + new String(expression) + "} in position " + p);
		}
		option(expression, right + 1);
	}

	private void option(String expression, int p) {
		// #{property,javaType=int,jdbcType=NUMERIC}
		int left = skipWS(expression, p);
		if (left < expression.length()) {
			// 获取=号之前的位置
			int right = skipUntil(expression, left, "=");
			String name = trimmedStr(expression, left, right);
			left = right + 1;
			right = skipUntil(expression, left, ",");
			String value = trimmedStr(expression, left, right);
			put(name, value);
			// 递归调用option，进行逗号后面一个属性的解析
			option(expression, right + 1);
		}
	}

	private String trimmedStr(String str, int start, int end) {
		while (str.charAt(start) <= 0x20) {
			start++;
		}
		while (str.charAt(end - 1) <= 0x20) {
			end--;
		}
		return start >= end ? "" : str.substring(start, end);
	}

}
