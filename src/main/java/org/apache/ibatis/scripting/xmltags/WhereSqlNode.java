package org.apache.ibatis.scripting.xmltags;

import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.session.Configuration;

public class WhereSqlNode extends TrimSqlNode {

	private static List<String> prefixList = Arrays.asList("AND ", "OR ", "AND\n", "OR\n", "AND\r", "OR\r", "AND\t", "OR\t");

	public WhereSqlNode(Configuration configuration, SqlNode contents) {
		super(configuration, contents, "WHERE", prefixList, null, null);
	}

}
