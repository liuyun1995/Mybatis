package org.apache.ibatis.builder.xml;

import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//XML include转换器
public class XMLIncludeTransformer {

	private final Configuration configuration;              //配置信息
	private final MapperBuilderAssistant builderAssistant;  //构建助手

	public XMLIncludeTransformer(Configuration configuration, MapperBuilderAssistant builderAssistant) {
		this.configuration = configuration;
		this.builderAssistant = builderAssistant;
	}
	
	//解析结点中包含的<include>标签
	public void applyIncludes(Node source) {
		if (source.getNodeName().equals("include")) {
			//根据refid属性值获取相应结点
			Node toInclude = findSqlFragment(getStringAttribute(source, "refid"));
			//递归解析指向结点中的<include>标签
			applyIncludes(toInclude);
			//判断两个结点是否属于同一XML文档
			if (toInclude.getOwnerDocument() != source.getOwnerDocument()) {
				//将指向结点导入到原结点文档
				toInclude = source.getOwnerDocument().importNode(toInclude, true);
			}
			//将原结点替换成指向结点
			source.getParentNode().replaceChild(toInclude, source);
			//若指向结点存在子结点, 则将子结点插到指向结点前面
			//这里主要是将指向结点中所有非include的子结点移到指向结点前面
			while (toInclude.hasChildNodes()) {
				//将子结点插入到指向结点前面
				toInclude.getParentNode().insertBefore(toInclude.getFirstChild(), toInclude);
			}
			//删除原结点的子结点
			toInclude.getParentNode().removeChild(toInclude);
		} else if (source.getNodeType() == Node.ELEMENT_NODE) {
			//获取原结点所有子结点
			NodeList children = source.getChildNodes();
			//递归解析所有子结点
			for (int i = 0; i < children.getLength(); i++) {
				applyIncludes(children.item(i));
			}
		}
	}

	//根据id获取指定的结点
	private Node findSqlFragment(String refid) {
		//获取文本里面的属性变量
		refid = PropertyParser.parse(refid, configuration.getVariables());
		//加上当前namespace前缀
		refid = builderAssistant.applyCurrentNamespace(refid, true);
		try {
			//在配置信息中的sql映射片段中获取指定结点
			XNode nodeToInclude = configuration.getSqlFragments().get(refid);
			//返回该node的克隆值
			return nodeToInclude.getNode().cloneNode(true);
		} catch (IllegalArgumentException e) {
			throw new IncompleteElementException("Could not find SQL statement to include with refid '" + refid + "'", e);
		}
	}

	//获取指定名称的属性值
	private String getStringAttribute(Node node, String name) {
		return node.getAttributes().getNamedItem(name).getNodeValue();
	}
	
}
