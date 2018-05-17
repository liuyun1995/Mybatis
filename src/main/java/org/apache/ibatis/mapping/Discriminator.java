package org.apache.ibatis.mapping;

import java.util.Collections;
import java.util.Map;

import org.apache.ibatis.session.Configuration;

//鉴别器。有时一个查询也许返回很多不同数据类型的结果集。 鉴别器的表现很像 Java 语言中的 switch 语句。
public class Discriminator {

	private ResultMapping resultMapping;
	private Map<String, String> discriminatorMap;

	Discriminator() {}

	public static class Builder {
		private Discriminator discriminator = new Discriminator();

		public Builder(Configuration configuration, ResultMapping resultMapping, Map<String, String> discriminatorMap) {
			discriminator.resultMapping = resultMapping;
			discriminator.discriminatorMap = discriminatorMap;
		}

		public Discriminator build() {
			assert discriminator.resultMapping != null;
			assert discriminator.discriminatorMap != null;
			assert !discriminator.discriminatorMap.isEmpty();
			discriminator.discriminatorMap = Collections.unmodifiableMap(discriminator.discriminatorMap);
			return discriminator;
		}
	}

	public ResultMapping getResultMapping() {
		return resultMapping;
	}

	public Map<String, String> getDiscriminatorMap() {
		return discriminatorMap;
	}

	public String getMapIdFor(String s) {
		return discriminatorMap.get(s);
	}

}
