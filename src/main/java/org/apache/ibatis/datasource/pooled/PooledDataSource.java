package org.apache.ibatis.datasource.pooled;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

//池化数据源
public class PooledDataSource implements DataSource {

	private static final Log log = LogFactory.getLog(PooledDataSource.class);

	private final PoolState state = new PoolState(this);    //连接池状态
	private final UnpooledDataSource dataSource;             //数据源
	protected int poolMaximumActiveConnections = 10;         //活动连接的最大数量
	protected int poolMaximumIdleConnections = 5;            //空闲连接的最大数量
	protected int poolMaximumCheckoutTime = 20000;           //最大检查时间
	protected int poolTimeToWait = 20000;                    //连接池等待时间
	protected String poolPingQuery = "NO PING QUERY SET";    //侦测查询字符串
	protected boolean poolPingEnabled = false;              //是否开启侦测查询
	protected int poolPingConnectionsNotUsedFor = 0;         //无用的侦测
	private int expectedConnectionTypeCode;                  //期望的连接类型

	//----------------------------------------------构造器----------------------------------------------
	
	//构造器1
	public PooledDataSource() {
		dataSource = new UnpooledDataSource();
	}

	//构造器2
	public PooledDataSource(String driver, String url, String username, String password) {
		dataSource = new UnpooledDataSource(driver, url, username, password);
		expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(),
				dataSource.getPassword());
	}

	//构造器3
	public PooledDataSource(String driver, String url, Properties driverProperties) {
		dataSource = new UnpooledDataSource(driver, url, driverProperties);
		expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(),
				dataSource.getPassword());
	}

	//构造器4
	public PooledDataSource(ClassLoader driverClassLoader, String driver, String url, String username,
			String password) {
		dataSource = new UnpooledDataSource(driverClassLoader, driver, url, username, password);
		expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(),
				dataSource.getPassword());
	}

	//构造器5
	public PooledDataSource(ClassLoader driverClassLoader, String driver, String url, Properties driverProperties) {
		dataSource = new UnpooledDataSource(driverClassLoader, driver, url, driverProperties);
		expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(),
				dataSource.getPassword());
	}
	
	//-------------------------------------------getter和setter-----------------------------------------

	//设置登录超时时间
	public void setLoginTimeout(int loginTimeout) throws SQLException {
		DriverManager.setLoginTimeout(loginTimeout);
	}

	//获取登录超时时间
	public int getLoginTimeout() throws SQLException {
		return DriverManager.getLoginTimeout();
	}

	//设置日志打印者
	public void setLogWriter(PrintWriter logWriter) throws SQLException {
		DriverManager.setLogWriter(logWriter);
	}

	//获取日志打印者
	public PrintWriter getLogWriter() throws SQLException {
		return DriverManager.getLogWriter();
	}

	//设置驱动名
	public void setDriver(String driver) {
		dataSource.setDriver(driver);
		forceCloseAll();
	}
	
	//获取驱动名
	public String getDriver() {
		return dataSource.getDriver();
	}

	//设置url
	public void setUrl(String url) {
		dataSource.setUrl(url);
		forceCloseAll();
	}
	
	//获取url
	public String getUrl() {
		return dataSource.getUrl();
	}

	//设置用户名
	public void setUsername(String username) {
		dataSource.setUsername(username);
		forceCloseAll();
	}
	
	//获取用户名
	public String getUsername() {
		return dataSource.getUsername();
	}

	//设置密码
	public void setPassword(String password) {
		dataSource.setPassword(password);
		forceCloseAll();
	}
	
	//获取密码
	public String getPassword() {
		return dataSource.getPassword();
	}

	//设置是否默认提交
	public void setDefaultAutoCommit(boolean defaultAutoCommit) {
		dataSource.setAutoCommit(defaultAutoCommit);
		forceCloseAll();
	}
	
	//获取是否默认提交
	public boolean isAutoCommit() {
		return dataSource.isAutoCommit();
	}

	//设置默认事务级别
	public void setDefaultTransactionIsolationLevel(Integer defaultTransactionIsolationLevel) {
		dataSource.setDefaultTransactionIsolationLevel(defaultTransactionIsolationLevel);
		forceCloseAll();
	}
	
	//获取默认事务级别
	public Integer getDefaultTransactionIsolationLevel() {
		return dataSource.getDefaultTransactionIsolationLevel();
	}

	//设置驱动属性
	public void setDriverProperties(Properties driverProps) {
		dataSource.setDriverProperties(driverProps);
		forceCloseAll();
	}
	
	//获取驱动属性
	public Properties getDriverProperties() {
		return dataSource.getDriverProperties();
	}

	//设置最大活跃连接数
	public void setPoolMaximumActiveConnections(int poolMaximumActiveConnections) {
		this.poolMaximumActiveConnections = poolMaximumActiveConnections;
		forceCloseAll();
	}
	
	//获取活跃连接数量
	public int getPoolMaximumActiveConnections() {
		return poolMaximumActiveConnections;
	}

	//设置最大空闲连接数
	public void setPoolMaximumIdleConnections(int poolMaximumIdleConnections) {
		this.poolMaximumIdleConnections = poolMaximumIdleConnections;
		forceCloseAll();
	}
	
	//获取空闲连接数量
	public int getPoolMaximumIdleConnections() {
		return poolMaximumIdleConnections;
	}
	
	//设置最大检出时间
	public void setPoolMaximumCheckoutTime(int poolMaximumCheckoutTime) {
		this.poolMaximumCheckoutTime = poolMaximumCheckoutTime;
		forceCloseAll();
	}
	
	//获取最大检出时间
	public int getPoolMaximumCheckoutTime() {
		return poolMaximumCheckoutTime;
	}
	
	//设置最大等待时间
	public void setPoolTimeToWait(int poolTimeToWait) {
		this.poolTimeToWait = poolTimeToWait;
		forceCloseAll();
	}
	
	//获取最大等待时间
	public int getPoolTimeToWait() {
		return poolTimeToWait;
	}
	
	//设置侦测查询字符串
	public void setPoolPingQuery(String poolPingQuery) {
		this.poolPingQuery = poolPingQuery;
		forceCloseAll();
	}
	
	//获取侦测查询字符串
	public String getPoolPingQuery() {
		return poolPingQuery;
	}
	
	//设置是否侦测查询
	public void setPoolPingEnabled(boolean poolPingEnabled) {
		this.poolPingEnabled = poolPingEnabled;
		forceCloseAll();
	}
	
	//获取是否侦测查询
	public boolean isPoolPingEnabled() {
		return poolPingEnabled;
	}
	
	public void setPoolPingConnectionsNotUsedFor(int milliseconds) {
		this.poolPingConnectionsNotUsedFor = milliseconds;
		forceCloseAll();
	}
	
	public int getPoolPingConnectionsNotUsedFor() {
		return poolPingConnectionsNotUsedFor;
	}
	
	//------------------------------------------------------------------------------------------------
	
	//获取数据库连接
	public Connection getConnection() throws SQLException {
		return popConnection(dataSource.getUsername(), dataSource.getPassword()).getProxyConnection();
	}

	//获取数据库连接(根据用户名和密码)
	public Connection getConnection(String username, String password) throws SQLException {
		return popConnection(username, password).getProxyConnection();
	}

	//关闭所有连接
	public void forceCloseAll() {
		synchronized (state) {
			expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(),
					dataSource.getPassword());
			// 关闭所有的activeConnections和idleConnections
			for (int i = state.activeConnections.size(); i > 0; i--) {
				try {
					PooledConnection conn = state.activeConnections.remove(i - 1);
					conn.invalidate();

					Connection realConn = conn.getRealConnection();
					if (!realConn.getAutoCommit()) {
						realConn.rollback();
					}
					realConn.close();
				} catch (Exception e) {
					// ignore
				}
			}
			for (int i = state.idleConnections.size(); i > 0; i--) {
				try {
					PooledConnection conn = state.idleConnections.remove(i - 1);
					conn.invalidate();

					Connection realConn = conn.getRealConnection();
					if (!realConn.getAutoCommit()) {
						realConn.rollback();
					}
					realConn.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
		if (log.isDebugEnabled()) {
			log.debug("PooledDataSource forcefully closed/removed all connections.");
		}
	}

	//获取连接池状态
	public PoolState getPoolState() {
		return state;
	}

	//组合连接类型代码
	private int assembleConnectionTypeCode(String url, String username, String password) {
		return ("" + url + username + password).hashCode();
	}

	//放入一个连接
	protected void pushConnection(PooledConnection conn) throws SQLException {
		synchronized (state) {
			//先从活动连接列表中删除此连接
			state.activeConnections.remove(conn);
			//如果该连接是有效的
			if (conn.isValid()) {
				//如果空闲连接数小于最大空闲连接数
				if (state.idleConnections.size() < poolMaximumIdleConnections
						&& conn.getConnectionTypeCode() == expectedConnectionTypeCode) {
					//将原连接的检查时间叠加
					state.accumulatedCheckoutTime += conn.getCheckoutTime();
					//回滚连接之前的事务
					if (!conn.getRealConnection().getAutoCommit()) {
						conn.getRealConnection().rollback();
					}
					//新建一个连接
					PooledConnection newConn = new PooledConnection(conn.getRealConnection(), this);
					//将新连接放入空闲列表
					state.idleConnections.add(newConn);
					//设置新连接的创建时间
					newConn.setCreatedTimestamp(conn.getCreatedTimestamp());
					//设置新连接的最后使用时间
					newConn.setLastUsedTimestamp(conn.getLastUsedTimestamp());
					//使原连接失效
					conn.invalidate();
					if (log.isDebugEnabled()) {
						log.debug("Returned connection " + newConn.getRealHashCode() + " to pool.");
					}
					//唤醒所有在该条件阻塞的线程
					state.notifyAll();
				} else {
					//将原连接的检查时间叠加
					state.accumulatedCheckoutTime += conn.getCheckoutTime();
					//回滚连接之前的事务
					if (!conn.getRealConnection().getAutoCommit()) {
						conn.getRealConnection().rollback();
					}
					//将数据库连接关闭
					conn.getRealConnection().close();
					if (log.isDebugEnabled()) {
						log.debug("Closed connection " + conn.getRealHashCode() + ".");
					}
					//使连接失效
					conn.invalidate();
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug("A bad connection (" + conn.getRealHashCode()
							+ ") attempted to return to the pool, discarding connection.");
				}
				//坏的连接数加一
				state.badConnectionCount++;
			}
		}
	}

	//获取一个连接
	private PooledConnection popConnection(String username, String password) throws SQLException {
		boolean countedWait = false;
		PooledConnection conn = null;
		long t = System.currentTimeMillis();
		int localBadConnectionCount = 0;
		while (conn == null) {
			//以连接池状态作为锁
			synchronized (state) {
				//如果有空闲连接的话
				if (!state.idleConnections.isEmpty()) {
					//获取空闲列表的第一个连接
					conn = state.idleConnections.remove(0);
					if (log.isDebugEnabled()) {
						log.debug("Checked out connection " + conn.getRealHashCode() + " from pool.");
					}
				//如果没有空闲的连接
				} else {
					//如果活跃连接数小于最大活跃连接数
					if (state.activeConnections.size() < poolMaximumActiveConnections) {
						//新建一个池化连接
						conn = new PooledConnection(dataSource.getConnection(), this);
						if (log.isDebugEnabled()) {
							log.debug("Created connection " + conn.getRealHashCode() + ".");
						}
					} else {
						//否则就复用活跃列表的连接
						PooledConnection oldestActiveConnection = state.activeConnections.get(0);
						//获取检出时间
						long longestCheckoutTime = oldestActiveConnection.getCheckoutTime();
						//如果检出时间大于最大检出时间
						if (longestCheckoutTime > poolMaximumCheckoutTime) {
							state.claimedOverdueConnectionCount++;
							state.accumulatedCheckoutTimeOfOverdueConnections += longestCheckoutTime;
							state.accumulatedCheckoutTime += longestCheckoutTime;
							state.activeConnections.remove(oldestActiveConnection);
							//将连接中的所有事务回滚
							if (!oldestActiveConnection.getRealConnection().getAutoCommit()) {
								oldestActiveConnection.getRealConnection().rollback();
							}
							//复用真正连接, 新建池化连接
							conn = new PooledConnection(oldestActiveConnection.getRealConnection(), this);
							//使旧的池化连接无效
							oldestActiveConnection.invalidate();
							if (log.isDebugEnabled()) {
								log.debug("Claimed overdue connection " + conn.getRealHashCode() + ".");
							}
						} else {
							//若检出时间小于最大检出时间, 则进行等待
							try {
								//若没人在等待
								if (!countedWait) {
									//等待次数加1
									state.hadToWaitCount++;
									countedWait = true;
								}
								if (log.isDebugEnabled()) {
									log.debug("Waiting as long as " + poolTimeToWait + " milliseconds for connection.");
								}
								//获取当前时间
								long wt = System.currentTimeMillis();
								//将当前线程阻塞在state条件上
								state.wait(poolTimeToWait);
								//线程醒来统计总等待时间
								state.accumulatedWaitTime += System.currentTimeMillis() - wt;
							} catch (InterruptedException e) {
								break;
							}
						}
					}
				}
				if (conn != null) {
					//若连接是有效的
					if (conn.isValid()) {
						//回滚之前的事务
						if (!conn.getRealConnection().getAutoCommit()) {
							conn.getRealConnection().rollback();
						}
						//设置连接类型代码
						conn.setConnectionTypeCode(assembleConnectionTypeCode(dataSource.getUrl(), username, password));
						//设置检出时间
						conn.setCheckoutTimestamp(System.currentTimeMillis());
						//设置最近使用时间
						conn.setLastUsedTimestamp(System.currentTimeMillis());
						//将该连接添加到活跃列表
						state.activeConnections.add(conn);
						state.requestCount++;
						state.accumulatedRequestTime += System.currentTimeMillis() - t;
					} else {
						if (log.isDebugEnabled()) {
							log.debug("A bad connection (" + conn.getRealHashCode()
									+ ") was returned from the pool, getting another connection.");
						}
						//坏的连接数加1
						state.badConnectionCount++;
						//本地坏的连接加1
						localBadConnectionCount++;
						//将池化连接置空
						conn = null;
						//若坏连接数超过一定值, 则抛出异常
						if (localBadConnectionCount > (poolMaximumIdleConnections + 3)) {
							if (log.isDebugEnabled()) {
								log.debug("PooledDataSource: Could not get a good connection to the database.");
							}
							throw new SQLException(
									"PooledDataSource: Could not get a good connection to the database.");
						}
					}
				}
			}
		}
		//跳出循坏后, 若连接为空则抛出异常
		if (conn == null) {
			if (log.isDebugEnabled()) {
				log.debug(
						"PooledDataSource: Unknown severe error condition.  The connection pool returned a null connection.");
			}
			throw new SQLException(
					"PooledDataSource: Unknown severe error condition.  The connection pool returned a null connection.");
		}
		//返回连接
		return conn;
	}

	//探测连接
	protected boolean pingConnection(PooledConnection conn) {
		boolean result = true;
		try {
			//获取连接是否已经关闭
			result = !conn.getRealConnection().isClosed();
		} catch (SQLException e) {
			if (log.isDebugEnabled()) {
				log.debug("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
			}
			result = false;
		}
		//如果连接没有关闭
		if (result) {
			if (poolPingEnabled) {
				if (poolPingConnectionsNotUsedFor >= 0
						&& conn.getTimeElapsedSinceLastUse() > poolPingConnectionsNotUsedFor) {
					try {
						if (log.isDebugEnabled()) {
							log.debug("Testing connection " + conn.getRealHashCode() + " ...");
						}
						//获取数据库连接
						Connection realConn = conn.getRealConnection();
						//获取jdbc语句
						Statement statement = realConn.createStatement();
						//用侦测查询字符串查询
						ResultSet rs = statement.executeQuery(poolPingQuery);
						rs.close();
						statement.close();
						//如果连接不是自动提交就回滚
						if (!realConn.getAutoCommit()) {
							realConn.rollback();
						}
						result = true;
						if (log.isDebugEnabled()) {
							log.debug("Connection " + conn.getRealHashCode() + " is GOOD!");
						}
					} catch (Exception e) {
						log.warn("Execution of ping query '" + poolPingQuery + "' failed: " + e.getMessage());
						try {
							conn.getRealConnection().close();
						} catch (Exception e2) {
							// ignore
						}
						result = false;
						if (log.isDebugEnabled()) {
							log.debug("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
						}
					}
				}
			}
		}
		return result;
	}

	//拆包池化连接
	public static Connection unwrapConnection(Connection conn) {
		if (Proxy.isProxyClass(conn.getClass())) {
			InvocationHandler handler = Proxy.getInvocationHandler(conn);
			if (handler instanceof PooledConnection) {
				return ((PooledConnection) handler).getRealConnection();
			}
		}
		return conn;
	}

	//销毁方法
	protected void finalize() throws Throwable {
		forceCloseAll();
		super.finalize();
	}

	//拆包方法
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new SQLException(getClass().getName() + " is not a wrapper.");
	}
	
	//是否从某类拆包而来
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	//获取父级日志
	public Logger getParentLogger() {
		return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	}

}
