package org.apache.ibatis.reflection.property;

import java.util.Iterator;

//属性分词器，迭代子模式， 如person[0].birthdate.year，将依次取得person[0], birthdate, year
public class PropertyTokenizer implements Iterable<PropertyTokenizer>, Iterator<PropertyTokenizer> {
	//例子:person[0].birthdate.year
	private String name;        //person
	private String indexedName; //person[0]
	private String index;       //0
	private String children;    //birthdate.year

	public PropertyTokenizer(String fullname) {
		// person[0].birthdate.year
		// 找.
		int delim = fullname.indexOf('.');
		if (delim > -1) {
			name = fullname.substring(0, delim);
			children = fullname.substring(delim + 1);
		} else {
			// 找不到.的话，取全部部分
			name = fullname;
			children = null;
		}
		indexedName = name;
		// 把中括号里的数字给解析出来
		delim = name.indexOf('[');
		if (delim > -1) {
			index = name.substring(delim + 1, name.length() - 1);
			name = name.substring(0, delim);
		}
	}

	public String getName() {
		return name;
	}

	public String getIndex() {
		return index;
	}

	public String getIndexedName() {
		return indexedName;
	}

	public String getChildren() {
		return children;
	}

	public boolean hasNext() {
		return children != null;
	}

	// 取得下一个,非常简单，直接再通过儿子来new另外一个实例
	public PropertyTokenizer next() {
		return new PropertyTokenizer(children);
	}

	public void remove() {
		throw new UnsupportedOperationException(
				"Remove is not supported, as it has no meaning in the context of properties.");
	}

	public Iterator<PropertyTokenizer> iterator() {
		return this;
	}
}
