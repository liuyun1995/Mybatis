package org.apache.ibatis.scripting.xmltags;

import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.session.Configuration;

public class SetSqlNode extends TrimSqlNode {

	private static List<String> suffixList = Arrays.asList(",");

	public SetSqlNode(Configuration configuration, SqlNode contents) {
		super(configuration, contents, "SET", null, null, suffixList);
	}

}
