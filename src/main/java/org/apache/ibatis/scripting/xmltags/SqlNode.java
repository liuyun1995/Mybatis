package org.apache.ibatis.scripting.xmltags;

/**
 * SQL节点（choose|foreach|if|）
 */
public interface SqlNode {

	boolean apply(DynamicContext context);

}
