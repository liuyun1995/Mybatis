package org.apache.ibatis.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XNode {
	
	private Node node;                  //dom结点
	private String name;                //dom结点名
	private String body;                //dom结点体
	private Properties attributes;      //dom结点属性集合
	private Properties variables;       //变量属性
	private XPathParser xpathParser;    //XPath解析器
	
	public XNode(XPathParser xpathParser, Node node, Properties variables) {
		this.xpathParser = xpathParser;
		this.node = node;
		this.name = node.getNodeName();
		this.variables = variables;
		this.attributes = parseAttributes(node);
		this.body = parseBody(node);
	}

	public XNode newXNode(Node node) {
		return new XNode(xpathParser, node, variables);
	}

	//获取父结点
	public XNode getParent() {
		Node parent = node.getParentNode();
		if (parent == null || !(parent instanceof Element)) {
			return null;
		} else {
			return new XNode(xpathParser, parent, variables);
		}
	}

	//获取结点路径
	public String getPath() {
		StringBuilder builder = new StringBuilder();
		Node current = node;
		while (current != null && current instanceof Element) {
			if (current != node) {
				builder.insert(0, "/");
			}
			builder.insert(0, current.getNodeName());
			current = current.getParentNode();
		}
		return builder.toString();
	}

	// 取得标示符 ("resultMap[authorResult]")
	// XMLMapperBuilder.resultMapElement调用
	// <resultMap id="authorResult" type="Author">
	// <id property="id" column="author_id"/>
	// <result property="username" column="author_username"/>
	// <result property="password" column="author_password"/>
	// <result property="email" column="author_email"/>
	// <result property="bio" column="author_bio"/>
	// </resultMap>
	public String getValueBasedIdentifier() {
		StringBuilder builder = new StringBuilder();
		XNode current = this;
		while (current != null) {
			if (current != this) {
				builder.insert(0, "_");
			}
			//先拿id, 拿不到再拿value, 再拿不到拿property
			String value = current.getStringAttribute("id",
					current.getStringAttribute("value", current.getStringAttribute("property", null)));
			if (value != null) {
				value = value.replace('.', '_');
				builder.insert(0, "]");
				builder.insert(0, value);
				builder.insert(0, "[");
			}
			builder.insert(0, current.getName());
			current = current.getParent();
		}
		return builder.toString();
	}

	//根据表达式获取String值
	public String evalString(String expression) {
		return xpathParser.evalString(node, expression);
	}

	//根据表达式获取Boolean值
	public Boolean evalBoolean(String expression) {
		return xpathParser.evalBoolean(node, expression);
	}

	//根据表达式获取Double值
	public Double evalDouble(String expression) {
		return xpathParser.evalDouble(node, expression);
	}

	//根据表达式获取XNode列表
	public List<XNode> evalNodes(String expression) {
		return xpathParser.evalNodes(node, expression);
	}

	//根据表达式获取XNode
	public XNode evalNode(String expression) {
		return xpathParser.evalNode(node, expression);
	}

	//获取原生Node
	public Node getNode() {
		return node;
	}

	//获取名字
	public String getName() {
		return name;
	}

	//获取String类型的内容
	public String getStringBody() {
		return getStringBody(null);
	}

	//获取String类型的内容
	public String getStringBody(String def) {
		if (body == null) {
			return def;
		} else {
			return body;
		}
	}

	//获取Boolean类型的内容
	public Boolean getBooleanBody() {
		return getBooleanBody(null);
	}

	//获取Boolean类型的内容
	public Boolean getBooleanBody(Boolean def) {
		if (body == null) {
			return def;
		} else {
			return Boolean.valueOf(body);
		}
	}

	//获取Integer类型的内容
	public Integer getIntBody() {
		return getIntBody(null);
	}

	//获取Integer类型的内容
	public Integer getIntBody(Integer def) {
		if (body == null) {
			return def;
		} else {
			return Integer.parseInt(body);
		}
	}

	//获取Long类型的内容
	public Long getLongBody() {
		return getLongBody(null);
	}

	//获取Long类型的内容
	public Long getLongBody(Long def) {
		if (body == null) {
			return def;
		} else {
			return Long.parseLong(body);
		}
	}

	//获取Double类型的内容
	public Double getDoubleBody() {
		return getDoubleBody(null);
	}

	//获取Double类型的内容
	public Double getDoubleBody(Double def) {
		if (body == null) {
			return def;
		} else {
			return Double.parseDouble(body);
		}
	}

	//获取Float类型的内容
	public Float getFloatBody() {
		return getFloatBody(null);
	}

	//获取Float类型的内容
	public Float getFloatBody(Float def) {
		if (body == null) {
			return def;
		} else {
			return Float.parseFloat(body);
		}
	}

	//获取enum属性值
	public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name) {
		return getEnumAttribute(enumType, name, null);
	}

	//获取enum属性值(带缺省值)
	public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name, T def) {
		String value = getStringAttribute(name);
		if (value == null) {
			return def;
		} else {
			return Enum.valueOf(enumType, value);
		}
	}

	//获取string属性值
	public String getStringAttribute(String name) {
		return getStringAttribute(name, null);
	}

	//获取string属性值(带缺省值)
	public String getStringAttribute(String name, String def) {
		String value = attributes.getProperty(name);
		if (value == null) {
			return def;
		} else {
			return value;
		}
	}

	//获取boolean属性值
	public Boolean getBooleanAttribute(String name) {
		return getBooleanAttribute(name, null);
	}

	//获取boolean属性值(带缺省值)
	public Boolean getBooleanAttribute(String name, Boolean def) {
		String value = attributes.getProperty(name);
		if (value == null) {
			return def;
		} else {
			return Boolean.valueOf(value);
		}
	}

	//获取Integer属性值
	public Integer getIntAttribute(String name) {
		return getIntAttribute(name, null);
	}

	//获取Integer属性值(带缺省值)
	public Integer getIntAttribute(String name, Integer def) {
		String value = attributes.getProperty(name);
		if (value == null) {
			return def;
		} else {
			return Integer.parseInt(value);
		}
	}

	//获取Long属性值
	public Long getLongAttribute(String name) {
		return getLongAttribute(name, null);
	}

	//获取Long属性值(带缺省值)
	public Long getLongAttribute(String name, Long def) {
		String value = attributes.getProperty(name);
		if (value == null) {
			return def;
		} else {
			return Long.parseLong(value);
		}
	}

	//获取Double属性值
	public Double getDoubleAttribute(String name) {
		return getDoubleAttribute(name, null);
	}

	//获取Double属性值(带缺省值)
	public Double getDoubleAttribute(String name, Double def) {
		String value = attributes.getProperty(name);
		if (value == null) {
			return def;
		} else {
			return Double.parseDouble(value);
		}
	}

	//获取Float属性值
	public Float getFloatAttribute(String name) {
		return getFloatAttribute(name, null);
	}

	//获取Float属性值(带缺省值)
	public Float getFloatAttribute(String name, Float def) {
		String value = attributes.getProperty(name);
		if (value == null) {
			return def;
		} else {
			return Float.parseFloat(value);
		}
	}

	//获取子节点列表
	public List<XNode> getChildren() {
		List<XNode> children = new ArrayList<XNode>();
		NodeList nodeList = node.getChildNodes();
		if (nodeList != null) {
			for (int i = 0, n = nodeList.getLength(); i < n; i++) {
				Node node = nodeList.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					//包装成XNode并放入列表中
					children.add(new XNode(xpathParser, node, variables));
				}
			}
		}
		return children;
	}

	//获取子节点属性
	public Properties getChildrenAsProperties() {
		Properties properties = new Properties();
		//遍历子节点列表
		for (XNode child : getChildren()) {
			//获取name属性值
			String name = child.getStringAttribute("name");
			//获取value属性值
			String value = child.getStringAttribute("value");
			//放入properties中
			if (name != null && value != null) {
				properties.setProperty(name, value);
			}
		}
		return properties;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("<");
		builder.append(name);
		for (Map.Entry<Object, Object> entry : attributes.entrySet()) {
			builder.append(" ");
			builder.append(entry.getKey());
			builder.append("=\"");
			builder.append(entry.getValue());
			builder.append("\"");
		}
		List<XNode> children = getChildren();
		if (!children.isEmpty()) {
			builder.append(">\n");
			for (XNode node : children) {
				builder.append(node.toString());
			}
			builder.append("</");
			builder.append(name);
			builder.append(">");
		} else if (body != null) {
			builder.append(">");
			builder.append(body);
			builder.append("</");
			builder.append(name);
			builder.append(">");
		} else {
			builder.append("/>");
		}
		builder.append("\n");
		return builder.toString();
	}

	//解析结点的属性
	private Properties parseAttributes(Node n) {
		Properties attributes = new Properties();
		//获取dom属性节点集合
		NamedNodeMap attributeNodes = n.getAttributes();
		if (attributeNodes != null) {
			for (int i = 0; i < attributeNodes.getLength(); i++) {
				//获取dom属性结点
				Node attribute = attributeNodes.item(i);
				//解析dom属性结点对应的值
				String value = PropertyParser.parse(attribute.getNodeValue(), variables);
				//将键值对放入attributes中
				attributes.put(attribute.getNodeName(), value);
			}
		}
		return attributes;
	}

	//解析结点体
	private String parseBody(Node node) {
		//获取结点体的数据
		String data = getBodyData(node);
		if (data == null) {
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				data = getBodyData(child);
				if (data != null) {
					break;
				}
			}
		}
		return data;
	}

	//获取结点体数据
	private String getBodyData(Node child) {
		//如果是CDATA类型或文本类型, 则解析出数据
		if (child.getNodeType() == Node.CDATA_SECTION_NODE || child.getNodeType() == Node.TEXT_NODE) {
			String data = ((CharacterData) child).getData();
			data = PropertyParser.parse(data, variables);
			return data;
		}
		return null;
	}

}