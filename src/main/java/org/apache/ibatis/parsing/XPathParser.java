package org.apache.ibatis.parsing;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.ibatis.builder.BuilderException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

//XPath解析器
public class XPathParser {

	private Document document;
	private boolean validation;
	private EntityResolver entityResolver;
	private Properties variables;             //属性
	private XPath xpath;
	
	//以下构造器传入一个参数
	public XPathParser(String xml) {
		commonConstructor(false, null, null);
		this.document = createDocument(new InputSource(new StringReader(xml)));
	}

	public XPathParser(Reader reader) {
		commonConstructor(false, null, null);
		this.document = createDocument(new InputSource(reader));
	}

	public XPathParser(InputStream inputStream) {
		commonConstructor(false, null, null);
		this.document = createDocument(new InputSource(inputStream));
	}

	public XPathParser(Document document) {
		commonConstructor(false, null, null);
		this.document = document;
	}

	//以下构造器传入两个参数
	public XPathParser(String xml, boolean validation) {
		commonConstructor(validation, null, null);
		this.document = createDocument(new InputSource(new StringReader(xml)));
	}
	
	public XPathParser(Reader reader, boolean validation) {
		commonConstructor(validation, null, null);
		this.document = createDocument(new InputSource(reader));
	}
	
	public XPathParser(InputStream inputStream, boolean validation) {
		commonConstructor(validation, null, null);
		this.document = createDocument(new InputSource(inputStream));
	}
	
	public XPathParser(Document document, boolean validation) {
		commonConstructor(validation, null, null);
		this.document = document;
	}
	
	//以下构造器传入三个参数
	public XPathParser(String xml, boolean validation, Properties variables) {
		commonConstructor(validation, variables, null);
		this.document = createDocument(new InputSource(new StringReader(xml)));
	}

	public XPathParser(Reader reader, boolean validation, Properties variables) {
		commonConstructor(validation, variables, null);
		this.document = createDocument(new InputSource(reader));
	}

	public XPathParser(InputStream inputStream, boolean validation, Properties variables) {
		commonConstructor(validation, variables, null);
		this.document = createDocument(new InputSource(inputStream));
	}

	public XPathParser(Document document, boolean validation, Properties variables) {
		commonConstructor(validation, variables, null);
		this.document = document;
	}
	
	//以下构造器传入四个参数
	public XPathParser(String xml, boolean validation, Properties variables, EntityResolver entityResolver) {
		commonConstructor(validation, variables, entityResolver);
		this.document = createDocument(new InputSource(new StringReader(xml)));
	}

	public XPathParser(Reader reader, boolean validation, Properties variables, EntityResolver entityResolver) {
		commonConstructor(validation, variables, entityResolver);
		this.document = createDocument(new InputSource(reader));
	}

	public XPathParser(InputStream inputStream, boolean validation, Properties variables,
			EntityResolver entityResolver) {
		commonConstructor(validation, variables, entityResolver);
		this.document = createDocument(new InputSource(inputStream));
	}

	public XPathParser(Document document, boolean validation, Properties variables, EntityResolver entityResolver) {
		commonConstructor(validation, variables, entityResolver);
		this.document = document;
	}
	
	//设置变量属性
	public void setVariables(Properties variables) {
		this.variables = variables;
	}

	//根据表达式获取String类型值
	public String evalString(String expression) {
		return evalString(document, expression);
	}

	//根据表达式获取String类型值
	public String evalString(Object root, String expression) {
		//1.先用xpath解析
		String result = (String) evaluate(expression, root, XPathConstants.STRING);
		//2.再调用PropertyParser去解析,也就是替换 ${}这种格式的字符串
		result = PropertyParser.parse(result, variables);
		return result;
	}

	//根据表达式获取Boolean类型值
	public Boolean evalBoolean(String expression) {
		return evalBoolean(document, expression);
	}

	//根据表达式获取Boolean类型值
	public Boolean evalBoolean(Object root, String expression) {
		return (Boolean) evaluate(expression, root, XPathConstants.BOOLEAN);
	}

	//根据表达式获取Short类型值
	public Short evalShort(String expression) {
		return evalShort(document, expression);
	}

	//根据表达式获取Short类型值
	public Short evalShort(Object root, String expression) {
		return Short.valueOf(evalString(root, expression));
	}

	//根据表达式获取Integer类型值
	public Integer evalInteger(String expression) {
		return evalInteger(document, expression);
	}

	//根据表达式获取Integer类型值
	public Integer evalInteger(Object root, String expression) {
		return Integer.valueOf(evalString(root, expression));
	}

	//根据表达式获取Long类型值
	public Long evalLong(String expression) {
		return evalLong(document, expression);
	}

	//根据表达式获取Long类型值
	public Long evalLong(Object root, String expression) {
		return Long.valueOf(evalString(root, expression));
	}

	//根据表达式获取Float类型值
	public Float evalFloat(String expression) {
		return evalFloat(document, expression);
	}

	//根据表达式获取Float类型值
	public Float evalFloat(Object root, String expression) {
		return Float.valueOf(evalString(root, expression));
	}

	//根据表达式获取Double类型值
	public Double evalDouble(String expression) {
		return evalDouble(document, expression);
	}

	//根据表达式获取Double类型值
	public Double evalDouble(Object root, String expression) {
		return (Double) evaluate(expression, root, XPathConstants.NUMBER);
	}

	//根据表达式获取XNode列表
	public List<XNode> evalNodes(String expression) {
		return evalNodes(document, expression);
	}

	//根据表达式获取XNode列表
	public List<XNode> evalNodes(Object root, String expression) {
		List<XNode> xnodes = new ArrayList<XNode>();
		NodeList nodes = (NodeList) evaluate(expression, root, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			xnodes.add(new XNode(this, nodes.item(i), variables));
		}
		return xnodes;
	}

	//根据表达式获取XNode
	public XNode evalNode(String expression) {
		return evalNode(document, expression);
	}

	//根据表达式获取XNode
	public XNode evalNode(Object root, String expression) {
		Node node = (Node) evaluate(expression, root, XPathConstants.NODE);
		if (node == null) {
			return null;
		}
		return new XNode(this, node, variables);
	}

	//返回xpath表达式的对象
	private Object evaluate(String expression, Object root, QName returnType) {
		try {
			return xpath.evaluate(expression, root, returnType);
		} catch (Exception e) {
			throw new BuilderException("Error evaluating XPath.  Cause: " + e, e);
		}
	}

	private Document createDocument(InputSource inputSource) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(validation);
			//名称空间
			factory.setNamespaceAware(false);
			//忽略注释
			factory.setIgnoringComments(true);
			//忽略空白
			factory.setIgnoringElementContentWhitespace(false);
			//将CDATA节点转换为Text节点
			factory.setCoalescing(false);
			//扩展实体引用
			factory.setExpandEntityReferences(true);

			DocumentBuilder builder = factory.newDocumentBuilder();
			// 需要注意的就是定义了EntityResolver(XMLMapperEntityResolver)，这样不用联网去获取DTD，
			// 将DTD放在org\apache\ibatis\builder\xml\mybatis-3-config.dtd,来达到验证xml合法性的目的
			builder.setEntityResolver(entityResolver);
			builder.setErrorHandler(new ErrorHandler() {
				public void error(SAXParseException exception) throws SAXException {
					throw exception;
				}
				public void fatalError(SAXParseException exception) throws SAXException {
					throw exception;
				}
				public void warning(SAXParseException exception) throws SAXException {
				}
			});
			return builder.parse(inputSource);
		} catch (Exception e) {
			throw new BuilderException("Error creating document instance.  Cause: " + e, e);
		}
	}

	private void commonConstructor(boolean validation, Properties variables, EntityResolver entityResolver) {
		this.validation = validation;
		this.entityResolver = entityResolver;
		this.variables = variables;
		//共通构造函数，除了把参数都设置到实例变量里面去以外，还初始化了XPath
		XPathFactory factory = XPathFactory.newInstance();
		this.xpath = factory.newXPath();
	}

}
