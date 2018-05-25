package org.apache.ibatis.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;

//SqlSessionFactory构建工厂
public class SqlSessionFactoryBuilder {
	
	//------------------------------------------------以下三组-----------------------------------------------

	public SqlSessionFactory build(Reader reader) {
		return build(reader, null, null);
	}

	public SqlSessionFactory build(Reader reader, String environment) {
		return build(reader, environment, null);
	}

	public SqlSessionFactory build(Reader reader, Properties properties) {
		return build(reader, null, properties);
	}

	//入口方法1
	public SqlSessionFactory build(Reader reader, String environment, Properties properties) {
		try {
			//新建XMLConfigBuilder对象解析xml文件
			XMLConfigBuilder parser = new XMLConfigBuilder(reader, environment, properties);
			//返回DefaultSqlSessionFactory
			return build(parser.parse());
		} catch (Exception e) {
			throw ExceptionFactory.wrapException("Error building SqlSession.", e);
		} finally {
			ErrorContext.instance().reset();
			try {
				reader.close();
			} catch (IOException e) {
			}
		}
	}
	
	//------------------------------------------------以下三组-----------------------------------------------
	
	public SqlSessionFactory build(InputStream inputStream) {
		return build(inputStream, null, null);
	}

	public SqlSessionFactory build(InputStream inputStream, String environment) {
		return build(inputStream, environment, null);
	}

	public SqlSessionFactory build(InputStream inputStream, Properties properties) {
		return build(inputStream, null, properties);
	}
	
	//入口方法2
	public SqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
		try {
			//新建XMLConfigBuilder对象解析xml文件
			XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);
			//返回DefaultSqlSessionFactory
			return build(parser.parse());
		} catch (Exception e) {
			throw ExceptionFactory.wrapException("Error building SqlSession.", e);
		} finally {
			ErrorContext.instance().reset();
			try {
				inputStream.close();
			} catch (IOException e) {
			}
		}
	}
	
	//------------------------------------------------------------------------------------------------------
	
	//根据配置信息构建SqlSessionFactory
	public SqlSessionFactory build(Configuration config) {
		//返回DefaultSqlSessionFactory
		return new DefaultSqlSessionFactory(config);
	}

}
