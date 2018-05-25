package org.apache.ibatis.parsing;

//通用记号解析器, 处理#{}和${}参数
public class GenericTokenParser {

	private final String openToken;     //开始记号
	private final String closeToken;    //结束记号
	private final TokenHandler handler; //记号处理器

	public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
		this.openToken = openToken;
		this.closeToken = closeToken;
		this.handler = handler;
	}

	//解析文本记号
	public String parse(String text) {
		StringBuilder builder = new StringBuilder();
		if (text != null && text.length() > 0) {
			char[] src = text.toCharArray();
			int offset = 0;
			//从offset开始找起, 返回第一次出现开始标记的位置
			int start = text.indexOf(openToken, offset);
			while (start > -1) {
				//判断一下开始标记之前是否有反斜杠
				if (start > 0 && src[start - 1] == '\\') {
					//若有反斜杠则不解析
					builder.append(src, offset, start - offset - 1).append(openToken);
					//将offset移至开始标记的后端
					offset = start + openToken.length();
				} else {
					//获取结束标记的位置
					int end = text.indexOf(closeToken, start);
					//如果没找到结束标记
					if (end == -1) {
						//将字符串中的开始标记截取掉
						builder.append(src, offset, src.length - offset);
						offset = src.length;
					} else {
						//截取括号外的字符串
						builder.append(src, offset, start - offset);
						//将offset指向开始标记的后端
						offset = start + openToken.length();
						//截取括号内字符串
						String content = new String(src, offset, end - offset);
						//先对括号内字符串进行变量替换, 再添加到返回串中
						builder.append(handler.handleToken(content));
						//将offset指向结束标记的后端
						offset = end + closeToken.length();
					}
				}
				//再次调整start位置, 寻找下一个括号
				start = text.indexOf(openToken, offset);
			}
			//添加字符串最后的部分
			if (offset < src.length) {
				builder.append(src, offset, src.length - offset);
			}
		}
		return builder.toString();
	}

}
