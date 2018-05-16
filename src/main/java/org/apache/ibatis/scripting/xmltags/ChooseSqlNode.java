package org.apache.ibatis.scripting.xmltags;

import java.util.List;

//choose SQL节点
public class ChooseSqlNode implements SqlNode {
	private SqlNode defaultSqlNode;
	private List<SqlNode> ifSqlNodes;

	public ChooseSqlNode(List<SqlNode> ifSqlNodes, SqlNode defaultSqlNode) {
		this.ifSqlNodes = ifSqlNodes;
		this.defaultSqlNode = defaultSqlNode;
	}

	public boolean apply(DynamicContext context) {
		// 循环判断if，只要有1个为true了，返回true
		for (SqlNode sqlNode : ifSqlNodes) {
			if (sqlNode.apply(context)) {
				return true;
			}
		}
		// if都不为true，那就看otherwise
		if (defaultSqlNode != null) {
			defaultSqlNode.apply(context);
			return true;
		}
		// 如果连otherwise都没有，返回false
		return false;
	}
}
