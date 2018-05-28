package org.apache.ibatis.builder;

import java.util.HashMap;

//参数表达式
public class ParameterExpression extends HashMap<String, String> {

	private static final long serialVersionUID = -2417552199605158680L;

	//构造器
	public ParameterExpression(String expression) {
		parse(expression);
	}

	//解析参数表达式
	private void parse(String expression) {
		//例子：#{property,javaType=int,jdbcType=NUMERIC}
		//去除空白并返回首个非空白字符的位置
		int p = skipWS(expression, 0);
		if (expression.charAt(p) == '(') {
			//若是"("则处理表达式
			expression(expression, p + 1);
		} else {
			//否则处理属性
			property(expression, p);
		}
	}

	//解析表达式
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
		//直接去最外面的括号内容, 放入到映射表
		put("expression", expression.substring(left, right - 1));
		//解析表达式后面的字符串
		jdbcTypeOpt(expression, right);
	}

	//解析属性
	private void property(String expression, int left) {
		if (left < expression.length()) {
			//获取首次出现逗号或者冒号的位置
			int right = skipUntil(expression, left, ",:");
			//以property为键, 参数名为值, 将其放入映射表中
			put("property", trimmedStr(expression, left, right));
			//解析参数名后面的字符串
			jdbcTypeOpt(expression, right);
		}
	}
	
	//解析参数名后面的字符串
	private void jdbcTypeOpt(String expression, int p) {
		//获取第一个不是空白的字符位置
		p = skipWS(expression, p);
		if (p < expression.length()) {
			if (expression.charAt(p) == ':') {
				//若是冒号, 则默认是jdbc类型
				jdbcType(expression, p + 1);
			} else if (expression.charAt(p) == ',') {
				//若是逗号则递归解析键值对
				option(expression, p + 1);
			} else {
				throw new BuilderException("Parsing error in {" + new String(expression) + "} in position " + p);
			}
		}
	}

	//获取jdbc类型
	private void jdbcType(String expression, int p) {
		//获取左边第一个非空格位置
		int left = skipWS(expression, p);
		//获取右边第一个逗号位置
		int right = skipUntil(expression, left, ",");
		if (right > left) {
			//截取并去除两端空格后, 放入映射表中
			put("jdbcType", trimmedStr(expression, left, right));
		} else {
			throw new BuilderException("Parsing error in {" + new String(expression) + "} in position " + p);
		}
		//解析后面的键值
		option(expression, right + 1);
	}

	//解析参数的键值对
	private void option(String expression, int p) {
		//获取左边第一个非空格位置
		int left = skipWS(expression, p);
		if (left < expression.length()) {
			//获取=号之前的位置
			int right = skipUntil(expression, left, "=");
			//获取操作名
			String name = trimmedStr(expression, left, right);
			left = right + 1;
			//从left开始获取第一个逗号的位置
			right = skipUntil(expression, left, ",");
			//获取操作值
			String value = trimmedStr(expression, left, right);
			//将键值放入到映射表中
			put(name, value);
			//递归解析逗号后面的属性
			option(expression, right + 1);
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

	//找到表达式中首个出现在endChars中的字符的位置
	private int skipUntil(String expression, int p, final String endChars) {
		//从位置p开始进行遍历
		for (int i = p; i < expression.length(); i++) {
			//取到表达式的第i个字符
			char c = expression.charAt(i);
			//若该字符在endChars中, 则返回它的位置
			if (endChars.indexOf(c) > -1) {
				return i;
			}
		}
		//否则返回原字符串长度
		return expression.length();
	}

	//截取字符串并去除两端空格
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
