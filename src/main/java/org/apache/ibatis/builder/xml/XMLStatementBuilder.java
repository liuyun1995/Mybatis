package org.apache.ibatis.builder.xml;

import java.util.List;
import java.util.Locale;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

//XML语句构建器
public class XMLStatementBuilder extends BaseBuilder {

	private MapperBuilderAssistant builderAssistant;  //构建助手
	private XNode context;                            //结点上下文
	private String requiredDatabaseId;

	public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode context) {
		this(configuration, builderAssistant, context, null);
	}

	public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode context,
			String databaseId) {
		super(configuration);
		this.builderAssistant = builderAssistant;
		this.context = context;
		this.requiredDatabaseId = databaseId;
	}

	//解析语句标签
	public void parseStatementNode() {
		//获取id属性值
		String id = context.getStringAttribute("id");
		//获取databaseId属性值
		String databaseId = context.getStringAttribute("databaseId");
		//如果databaseId不匹配则直接退出
		if (!databaseIdMatchesCurrent(id, databaseId, this.requiredDatabaseId)) {
			return;
		}
		//获取fetchSize属性值
		Integer fetchSize = context.getIntAttribute("fetchSize");
		//获取timeout属性值
		Integer timeout = context.getIntAttribute("timeout");
		//获取parameterMap属性值(已废弃)
		String parameterMap = context.getStringAttribute("parameterMap");
		//获取parameterType属性值
		String parameterType = context.getStringAttribute("parameterType");
		Class<?> parameterTypeClass = resolveClass(parameterType);
		//获取resultMap属性值
		String resultMap = context.getStringAttribute("resultMap");
		//获取resultType属性值
		String resultType = context.getStringAttribute("resultType");
		//获取lang属性值
		String lang = context.getStringAttribute("lang");
		//获取语言驱动
		LanguageDriver langDriver = getLanguageDriver(lang);
		//获取结果对象类型
		Class<?> resultTypeClass = resolveClass(resultType);
		//获取resultSetType属性
		String resultSetType = context.getStringAttribute("resultSetType");
		//获取语句类型, 默认为PREPARED
		StatementType statementType = StatementType.valueOf(context.getStringAttribute("statementType", StatementType.PREPARED.toString()));
		//根据resultSetType名获取结果集类型
		ResultSetType resultSetTypeEnum = resolveResultSetType(resultSetType);
		
		//获取结点标签名
		String nodeName = context.getNode().getNodeName();
		//通过结点名获取命令类型
		SqlCommandType sqlCommandType = SqlCommandType.valueOf(nodeName.toUpperCase(Locale.ENGLISH));
		boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
		//获取flushCache属性值
		boolean flushCache = context.getBooleanAttribute("flushCache", !isSelect);
		//获取useCache属性值
		boolean useCache = context.getBooleanAttribute("useCache", isSelect);
		//获取resultOrdered属性值
		boolean resultOrdered = context.getBooleanAttribute("resultOrdered", false);
		//解析之前先解析<include>标签
		XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, builderAssistant);
		includeParser.applyIncludes(context.getNode());
		//解析之前先解析<selectKey>标签
		processSelectKeyNodes(id, parameterTypeClass, langDriver);
		//解析成SqlSource, 一般是DynamicSqlSource
		SqlSource sqlSource = langDriver.createSqlSource(configuration, context, parameterTypeClass);
		//获取resultSets属性值
		String resultSets = context.getStringAttribute("resultSets");
		//获取keyProperty属性值
		String keyProperty = context.getStringAttribute("keyProperty");
		//获取keyColumn属性值
		String keyColumn = context.getStringAttribute("keyColumn");
		
		KeyGenerator keyGenerator;
		String keyStatementId = id + SelectKeyGenerator.SELECT_KEY_SUFFIX;
		keyStatementId = builderAssistant.applyCurrentNamespace(keyStatementId, true);
		//若配置信息中存在主键生成器则直接获取
		if (configuration.hasKeyGenerator(keyStatementId)) {
			keyGenerator = configuration.getKeyGenerator(keyStatementId);
	    //否则就生成一个主键生成器
		} else {
			//先取结点中useGeneratedKeys属性的值, 若未设置属性则再进行判断
			//默认使用Jdbc3KeyGenerator作为主键生成器
			keyGenerator = context.getBooleanAttribute("useGeneratedKeys",
					configuration.isUseGeneratedKeys() && SqlCommandType.INSERT.equals(sqlCommandType))
							? new Jdbc3KeyGenerator()
							: new NoKeyGenerator();
		}
		//调用构建助手进行构建
		builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout,
				parameterMap, parameterTypeClass, resultMap, resultTypeClass, resultSetTypeEnum, flushCache, useCache,
				resultOrdered, keyGenerator, keyProperty, keyColumn, databaseId, langDriver, resultSets);
	}

	private void processSelectKeyNodes(String id, Class<?> parameterTypeClass, LanguageDriver langDriver) {
		List<XNode> selectKeyNodes = context.evalNodes("selectKey");
		if (configuration.getDatabaseId() != null) {
			parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, configuration.getDatabaseId());
		}
		parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, null);
		removeSelectKeyNodes(selectKeyNodes);
	}

	private void parseSelectKeyNodes(String parentId, List<XNode> list, Class<?> parameterTypeClass,
			LanguageDriver langDriver, String skRequiredDatabaseId) {
		for (XNode nodeToHandle : list) {
			String id = parentId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
			String databaseId = nodeToHandle.getStringAttribute("databaseId");
			if (databaseIdMatchesCurrent(id, databaseId, skRequiredDatabaseId)) {
				parseSelectKeyNode(id, nodeToHandle, parameterTypeClass, langDriver, databaseId);
			}
		}
	}

	private void parseSelectKeyNode(String id, XNode nodeToHandle, Class<?> parameterTypeClass,
			LanguageDriver langDriver, String databaseId) {
		String resultType = nodeToHandle.getStringAttribute("resultType");
		Class<?> resultTypeClass = resolveClass(resultType);
		StatementType statementType = StatementType
				.valueOf(nodeToHandle.getStringAttribute("statementType", StatementType.PREPARED.toString()));
		String keyProperty = nodeToHandle.getStringAttribute("keyProperty");
		String keyColumn = nodeToHandle.getStringAttribute("keyColumn");
		boolean executeBefore = "BEFORE".equals(nodeToHandle.getStringAttribute("order", "AFTER"));

		// defaults
		boolean useCache = false;
		boolean resultOrdered = false;
		KeyGenerator keyGenerator = new NoKeyGenerator();
		Integer fetchSize = null;
		Integer timeout = null;
		boolean flushCache = false;
		String parameterMap = null;
		String resultMap = null;
		ResultSetType resultSetTypeEnum = null;

		SqlSource sqlSource = langDriver.createSqlSource(configuration, nodeToHandle, parameterTypeClass);
		SqlCommandType sqlCommandType = SqlCommandType.SELECT;

		builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout,
				parameterMap, parameterTypeClass, resultMap, resultTypeClass, resultSetTypeEnum, flushCache, useCache,
				resultOrdered, keyGenerator, keyProperty, keyColumn, databaseId, langDriver, null);

		id = builderAssistant.applyCurrentNamespace(id, false);

		MappedStatement keyStatement = configuration.getMappedStatement(id, false);
		configuration.addKeyGenerator(id, new SelectKeyGenerator(keyStatement, executeBefore));
	}

	private void removeSelectKeyNodes(List<XNode> selectKeyNodes) {
		for (XNode nodeToHandle : selectKeyNodes) {
			nodeToHandle.getParent().getNode().removeChild(nodeToHandle.getNode());
		}
	}

	private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
		if (requiredDatabaseId != null) {
			if (!requiredDatabaseId.equals(databaseId)) {
				return false;
			}
		} else {
			if (databaseId != null) {
				return false;
			}
			// skip this statement if there is a previous one with a not null databaseId
			id = builderAssistant.applyCurrentNamespace(id, false);
			if (this.configuration.hasStatement(id, false)) {
				MappedStatement previous = this.configuration.getMappedStatement(id, false); // issue #2
				if (previous.getDatabaseId() != null) {
					return false;
				}
			}
		}
		return true;
	}

	// 取得语言驱动
	private LanguageDriver getLanguageDriver(String lang) {
		Class<?> langClass = null;
		if (lang != null) {
			langClass = resolveClass(lang);
		}
		// 调用builderAssistant
		return builderAssistant.getLanguageDriver(langClass);
	}

}
