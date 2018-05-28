package org.apache.ibatis.datasource.pooled;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.ibatis.reflection.ExceptionUtil;

//池化连接
class PooledConnection implements InvocationHandler {

	private static final String CLOSE = "close";
	private static final Class<?>[] IFACES = new Class<?>[] { Connection.class };

	private int hashCode = 0;
	private PooledDataSource dataSource;       //池化数据源
	private Connection realConnection;         //真正的连接
	private Connection proxyConnection;        //代理的连接
	private long checkoutTimestamp;            //检出时间戳
	private long createdTimestamp;             //创建时间戳
	private long lastUsedTimestamp;            //上次使用时间戳
	private int connectionTypeCode;            //连接类型
	private boolean valid;                    //是否有效
	
	//构造器
	public PooledConnection(Connection connection, PooledDataSource dataSource) {
		this.hashCode = connection.hashCode();
		this.realConnection = connection;
		this.dataSource = dataSource;
		this.createdTimestamp = System.currentTimeMillis();
		this.lastUsedTimestamp = System.currentTimeMillis();
		this.valid = true;
		this.proxyConnection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), IFACES, this);
	}
	
	//使连接无效
	public void invalidate() {
		valid = false;
	}
	
	//检查连接是否有效
	public boolean isValid() {
		return valid && realConnection != null && dataSource.pingConnection(this);
	}

	//获取真实连接
	public Connection getRealConnection() {
		return realConnection;
	}

	//获取代理连接
	public Connection getProxyConnection() {
		return proxyConnection;
	}

	//获取真实哈希码
	public int getRealHashCode() {
		return realConnection == null ? 0 : realConnection.hashCode();
	}

	//获取连接类型
	public int getConnectionTypeCode() {
		return connectionTypeCode;
	}

	//设置连接类型
	public void setConnectionTypeCode(int connectionTypeCode) {
		this.connectionTypeCode = connectionTypeCode;
	}

	//获取连接建立的时间
	public long getCreatedTimestamp() {
		return createdTimestamp;
	}

	//设置连接建立的时间
	public void setCreatedTimestamp(long createdTimestamp) {
		this.createdTimestamp = createdTimestamp;
	}

	//获取最后使用的时间
	public long getLastUsedTimestamp() {
		return lastUsedTimestamp;
	}

	//设置最后使用的时间
	public void setLastUsedTimestamp(long lastUsedTimestamp) {
		this.lastUsedTimestamp = lastUsedTimestamp;
	}

	//获取当前时间到最后使用的时间间隔
	public long getTimeElapsedSinceLastUse() {
		return System.currentTimeMillis() - lastUsedTimestamp;
	}

	//获取连接持续的时间
	public long getAge() {
		return System.currentTimeMillis() - createdTimestamp;
	}

	//获取检出的时间
	public long getCheckoutTimestamp() {
		return checkoutTimestamp;
	}

	//设置检出的时间
	public void setCheckoutTimestamp(long timestamp) {
		this.checkoutTimestamp = timestamp;
	}

	//获取检出持续的时间
	public long getCheckoutTime() {
		return System.currentTimeMillis() - checkoutTimestamp;
	}
	
	//调用方法
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		String methodName = method.getName();
		// 如果调用close的话，忽略它，反而将这个connection加入到池中
		if (CLOSE.hashCode() == methodName.hashCode() && CLOSE.equals(methodName)) {
			dataSource.pushConnection(this);
			return null;
		} else {
			try {
				if (!Object.class.equals(method.getDeclaringClass())) {
					// 除了toString()方法，其他方法调用之前要检查connection是否还是合法的,不合法要抛出SQLException
					checkConnection();
				}
				// 其他的方法，则交给真正的connection去调用
				return method.invoke(realConnection, args);
			} catch (Throwable t) {
				throw ExceptionUtil.unwrapThrowable(t);
			}
		}
	}

	//检查连接是否有效
	private void checkConnection() throws SQLException {
		if (!valid) {
			throw new SQLException("Error accessing PooledConnection. Connection is invalid.");
		}
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PooledConnection) {
			return realConnection.hashCode() == (((PooledConnection) obj).realConnection.hashCode());
		} else if (obj instanceof Connection) {
			return hashCode == obj.hashCode();
		} else {
			return false;
		}
	}

}
