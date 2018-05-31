package org.apache.ibatis.binding;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;

//映射器方法
public class MapperMethod {

	private final SqlCommand command;       //方法命令
	private final MethodSignature method;   //方法签名

	//构造器
	public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
		this.command = new SqlCommand(config, mapperInterface, method);
		this.method = new MethodSignature(config, method);
	}

	//执行方法
	public Object execute(SqlSession sqlSession, Object[] args) {
		Object result;
		//执行insert方法
		if (SqlCommandType.INSERT == command.getType()) {
			Object param = method.convertArgsToSqlCommandParam(args);
			result = rowCountResult(sqlSession.insert(command.getName(), param));
		//执行update方法
		} else if (SqlCommandType.UPDATE == command.getType()) {
			Object param = method.convertArgsToSqlCommandParam(args);
			result = rowCountResult(sqlSession.update(command.getName(), param));
		//执行delete方法
		} else if (SqlCommandType.DELETE == command.getType()) {
			Object param = method.convertArgsToSqlCommandParam(args);
			result = rowCountResult(sqlSession.delete(command.getName(), param));
		//执行select方法
		} else if (SqlCommandType.SELECT == command.getType()) {
			//如果有结果处理器
			if (method.returnsVoid() && method.hasResultHandler()) {
				executeWithResultHandler(sqlSession, args);
				result = null;
			//如果结果有多条记录
			} else if (method.returnsMany()) {
				result = executeForMany(sqlSession, args);
			//如果结果是一个map
			} else if (method.returnsMap()) {
				result = executeForMap(sqlSession, args);
			//如果结果只有一条记录
			} else {
				Object param = method.convertArgsToSqlCommandParam(args);
				result = sqlSession.selectOne(command.getName(), param);
			}
		} else {
			throw new BindingException("Unknown execution method for: " + command.getName());
		}
		if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
			throw new BindingException("Mapper method '" + command.getName()
					+ " attempted to return null from a method with a primitive return type (" + method.getReturnType()
					+ ").");
		}
		return result;
	}

	//对返回值类型进行转换
	private Object rowCountResult(int rowCount) {
		final Object result;
		//如果返回值是void
		if (method.returnsVoid()) {
			result = null;
		//如果返回值是Integer或int
		} else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {
			result = Integer.valueOf(rowCount);
		//如果返回值是Long或long
		} else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {
			result = Long.valueOf(rowCount);
		//如果返回值是Boolean或boolean	
		} else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {
			result = Boolean.valueOf(rowCount > 0);
		//否则就抛出异常
		} else {
			throw new BindingException("Mapper method '" + command.getName() + "' has an unsupported return type: "
					+ method.getReturnType());
		}
		return result;
	}

	//执行带结果处理器的查询
	private void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
		MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
		if (void.class.equals(ms.getResultMaps().get(0).getType())) {
			throw new BindingException(
					"method " + command.getName() + " needs either a @ResultMap annotation, a @ResultType annotation,"
							+ " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
		}
		Object param = method.convertArgsToSqlCommandParam(args);
		if (method.hasRowBounds()) {
			RowBounds rowBounds = method.extractRowBounds(args);
			sqlSession.select(command.getName(), param, rowBounds, method.extractResultHandler(args));
		} else {
			sqlSession.select(command.getName(), param, method.extractResultHandler(args));
		}
	}

	//执行返回多条记录的查询
	private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
		List<E> result;
		Object param = method.convertArgsToSqlCommandParam(args);
		//如果方法有行范围(分页)
		if (method.hasRowBounds()) {
			//获取方法的行范围
			RowBounds rowBounds = method.extractRowBounds(args);
			result = sqlSession.<E>selectList(command.getName(), param, rowBounds);
		} else {
			result = sqlSession.<E>selectList(command.getName(), param);
		}
		//如果方法返回类型不是List
		if (!method.getReturnType().isAssignableFrom(result.getClass())) {
			//若返回类型是数组
			if (method.getReturnType().isArray()) {
				//将结果转为数组
				return convertToArray(result);
			} else {
				//将结果转成声明的集合
				return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
			}
		}
		return result;
	}

	private <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
		//生成返回类型的对象
		Object collection = config.getObjectFactory().create(method.getReturnType());
		//新建一个元对象
		MetaObject metaObject = config.newMetaObject(collection);
		metaObject.addAll(list);
		return collection;
	}

	//集合转数组
	@SuppressWarnings("unchecked")
	private <E> E[] convertToArray(List<E> list) {
		E[] array = (E[]) Array.newInstance(method.getReturnType().getComponentType(), list.size());
		array = list.toArray(array);
		return array;
	}

	//执行返回map的查询
	private <K, V> Map<K, V> executeForMap(SqlSession sqlSession, Object[] args) {
		Map<K, V> result;
		Object param = method.convertArgsToSqlCommandParam(args);
		if (method.hasRowBounds()) {
			RowBounds rowBounds = method.extractRowBounds(args);
			result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey(), rowBounds);
		} else {
			result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey());
		}
		return result;
	}

	//参数映射
	public static class ParamMap<V> extends HashMap<String, V> {

		private static final long serialVersionUID = -2212268410512043556L;

		@Override
		public V get(Object key) {
			if (!super.containsKey(key)) {
				throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + keySet());
			}
			return super.get(key);
		}

	}

	//SQL命令
	public static class SqlCommand {

		private final String name;
		private final SqlCommandType type;

		public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
			//获取语句名
			String statementName = mapperInterface.getName() + "." + method.getName();
			MappedStatement ms = null;
			//如果配置信息中存在该语句名, 则直接取出
			if (configuration.hasStatement(statementName)) {
				ms = configuration.getMappedStatement(statementName);
			} else if (!mapperInterface.equals(method.getDeclaringClass().getName())) {
				//如果不是这个mapper接口的方法，再去查父类
				String parentStatementName = method.getDeclaringClass().getName() + "." + method.getName();
				if (configuration.hasStatement(parentStatementName)) {
					ms = configuration.getMappedStatement(parentStatementName);
				}
			}
			if (ms == null) {
				throw new BindingException("Invalid bound statement (not found): " + statementName);
			}
			name = ms.getId();
			type = ms.getSqlCommandType();
			if (type == SqlCommandType.UNKNOWN) {
				throw new BindingException("Unknown execution method for: " + name);
			}
		}

		public String getName() {
			return name;
		}

		public SqlCommandType getType() {
			return type;
		}
	}

	//方法签名
	public static class MethodSignature {

		private final boolean returnsMany;                   //是否返回多个
		private final boolean returnsMap;                    //是否返回map
		private final boolean returnsVoid;                   //是否返回void
		private final Class<?> returnType;                    //返回类型
		private final String mapKey;
		private final Integer resultHandlerIndex;
		private final Integer rowBoundsIndex;
		private final SortedMap<Integer, String> params;
		private final boolean hasNamedParameters;

		//构造器
		public MethodSignature(Configuration configuration, Method method) {
			this.returnType = method.getReturnType();
			this.returnsVoid = void.class.equals(this.returnType);
			this.returnsMany = (configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray());
			this.mapKey = getMapKey(method);
			this.returnsMap = (this.mapKey != null);
			this.hasNamedParameters = hasNamedParams(method);
			this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
			this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);
			this.params = Collections.unmodifiableSortedMap(getParams(method, this.hasNamedParameters));
		}

		//将方法参数转换成sql命令参数
		public Object convertArgsToSqlCommandParam(Object[] args) {
			final int paramCount = params.size();
			//如果没有参数
			if (args == null || paramCount == 0) {
				return null;
			//如果只有一个参数
			} else if (!hasNamedParameters && paramCount == 1) {
				return args[params.keySet().iterator().next().intValue()];
			//如果存在多个参数
			} else {
				final Map<String, Object> param = new ParamMap<Object>();
				int i = 0;
				for (Map.Entry<Integer, String> entry : params.entrySet()) {
					//1.先加一个#{0},#{1},#{2}...参数
					param.put(entry.getValue(), args[entry.getKey().intValue()]);
					final String genericParamName = "param" + String.valueOf(i + 1);
					if (!param.containsKey(genericParamName)) {
						//2.再加一个#{param1},#{param2}...参数
						param.put(genericParamName, args[entry.getKey()]);
					}
					i++;
				}
				return param;
			}
		}

		//是否有RowBounds
		public boolean hasRowBounds() {
			return rowBoundsIndex != null;
		}

		//额外的RowBounds
		public RowBounds extractRowBounds(Object[] args) {
			return hasRowBounds() ? (RowBounds) args[rowBoundsIndex] : null;
		}

		//是否有结果处理器
		public boolean hasResultHandler() {
			return resultHandlerIndex != null;
		}

		//额外的结果处理器
		public ResultHandler extractResultHandler(Object[] args) {
			return hasResultHandler() ? (ResultHandler) args[resultHandlerIndex] : null;
		}

		//获取mapkey
		public String getMapKey() {
			return mapKey;
		}

		//获取返回类型
		public Class<?> getReturnType() {
			return returnType;
		}

		//是否返回多个值
		public boolean returnsMany() {
			return returnsMany;
		}

		//是否返回map
		public boolean returnsMap() {
			return returnsMap;
		}

		//是否返回void
		public boolean returnsVoid() {
			return returnsVoid;
		}

		//获取参数下标
		private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
			Integer index = null;
			//获取方法参数类型集合
			final Class<?>[] argTypes = method.getParameterTypes();
			//遍历参数类型
			for (int i = 0; i < argTypes.length; i++) {
				if (paramType.isAssignableFrom(argTypes[i])) {
					if (index == null) {
						index = i;
					} else {
						throw new BindingException(method.getName() + " cannot have multiple "
								+ paramType.getSimpleName() + " parameters");
					}
				}
			}
			return index;
		}

		private String getMapKey(Method method) {
			String mapKey = null;
			//方法返回类型是否为Map
			if (Map.class.isAssignableFrom(method.getReturnType())) {
				//查看方法是否有@MapKey注解, 若有该注解, 则将其值作为map的key
				final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
				if (mapKeyAnnotation != null) {
					mapKey = mapKeyAnnotation.value();
				}
			}
			return mapKey;
		}

		//获取所有参数
		private SortedMap<Integer, String> getParams(Method method, boolean hasNamedParameters) {
			//新建一个TreeMap
			final SortedMap<Integer, String> params = new TreeMap<Integer, String>();
			//获取方法的参数类型集合
			final Class<?>[] argTypes = method.getParameterTypes();
			//遍历所有参数类型
			for (int i = 0; i < argTypes.length; i++) {
				//若参数类型不是RowBounds或ResultHandler
				if (!RowBounds.class.isAssignableFrom(argTypes[i])
						&& !ResultHandler.class.isAssignableFrom(argTypes[i])) {
					//参数名字默认为0,1,2...
					String paramName = String.valueOf(params.size());
					//是否有命名的参数
					if (hasNamedParameters) {
						//从注解中获取参数名
						paramName = getParamNameFromAnnotation(method, i, paramName);
					}
					//放入序号和参数名的映射
					params.put(i, paramName);
				}
			}
			return params;
		}

		//从注解中获取参数名
		private String getParamNameFromAnnotation(Method method, int i, String paramName) {
			//获取第i个参数的注解
			final Object[] paramAnnos = method.getParameterAnnotations()[i];
			for (Object paramAnno : paramAnnos) {
				//如果为@Param注解
				if (paramAnno instanceof Param) {
					//参数名设置成@Param注解的值
					paramName = ((Param) paramAnno).value();
				}
			}
			//返回参数名
			return paramName;
		}

		//方法中是否有命名的参数
		private boolean hasNamedParams(Method method) {
			boolean hasNamedParams = false;
			//获取方法中所有参数的注解
			final Object[][] paramAnnos = method.getParameterAnnotations();
			//遍历所有参数的注解, 寻找是否存在@Param注解
			for (Object[] paramAnno : paramAnnos) {
				for (Object aParamAnno : paramAnno) {
					if (aParamAnno instanceof Param) {
						hasNamedParams = true;
						break;
					}
				}
			}
			return hasNamedParams;
		}

	}

}
