##### Mybatis方法参数处理

###### 接口式方法调用

```
BookDao bookDao = sqlSession.getMapper(BookDao.class);
selectList = bookDao.selectListAnnotation(idStr);
```

```
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	if (Object.class.equals(method.getDeclaringClass())) {
		return method.invoke(this, args);
	}
	final MapperMethod mapperMethod = cachedMapperMethod(method);
	return mapperMethod.execute(sqlSession, args);
}
```

```
public Object execute(SqlSession sqlSession, Object[] args) {
	Object result;
	if (SqlCommandType.INSERT == command.getType()) {
		Object param = method.convertArgsToSqlCommandParam(args);
		result = rowCountResult(sqlSession.insert(command.getName(), param));
	} else if (SqlCommandType.UPDATE == command.getType()) {
		Object param = method.convertArgsToSqlCommandParam(args);
		result = rowCountResult(sqlSession.update(command.getName(), param));
	} else if (SqlCommandType.DELETE == command.getType()) {
		Object param = method.convertArgsToSqlCommandParam(args);
		result = rowCountResult(sqlSession.delete(command.getName(), param));
	} else if (SqlCommandType.SELECT == command.getType()) {
		if (method.returnsVoid() && method.hasResultHandler()) {
			executeWithResultHandler(sqlSession, args);
			result = null;
		} else if (method.returnsMany()) {
			result = executeForMany(sqlSession, args);
		} else if (method.returnsMap()) {
			result = executeForMap(sqlSession, args);
		} else {
			Object param = method.convertArgsToSqlCommandParam(args);
			result = sqlSession.selectOne(command.getName(), param);
		}
	} else {
		throw new BindingException("Unknown execution method for: " + command.getName());
	}
	if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
		throw new BindingException("Mapper method '" + command.getName()
		                           + " attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
	}
	return result;
}
```

```
private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
	List<E> result;
	Object param = method.convertArgsToSqlCommandParam(args);
	if (method.hasRowBounds()) {
		RowBounds rowBounds = method.extractRowBounds(args);
		result = sqlSession.<E>selectList(command.getName(), param, rowBounds);
	} else {
		result = sqlSession.<E>selectList(command.getName(), param);
	}
	// issue #510 Collections & arrays support
	if (!method.getReturnType().isAssignableFrom(result.getClass())) {
		if (method.getReturnType().isArray()) {
			return convertToArray(result);
		} else {
			return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
		}
	}
	return result;
}
```

```
public Object convertArgsToSqlCommandParam(Object[] args) {
	final int paramCount = params.size();
	if (args == null || paramCount == 0) {
		return null;
	} else if (!hasNamedParameters && paramCount == 1) {
		return args[params.keySet().iterator().next()];
	} else {
		final Map<String, Object> param = new ParamMap<Object>();
		int i = 0;
	for (Map.Entry<Integer, String> entry : params.entrySet()) {
			param.put(entry.getValue(), args[entry.getKey()]);
			// issue #71, add param names as param1, param2...but ensure backward compatibility
			final String genericParamName = "param" + String.valueOf(i + 1);
			if (!param.containsKey(genericParamName)) {
				param.put(genericParamName, args[entry.getKey()]);
			}
			i++;
		}
		return param;
	}
}
```
1. 入参为null或没有，则返回null
2. 没有使用@Param注解并且参数只有一个，返回这个参数
3. 使用了@Param注解或多个参数，将参数转换为Map类型，如下方法
	```
	List<Book> selectListAnnotation(@Param("str") String idStr,String name);
	```
	则SortedMap<Integer, String> params中存储的就是:{0:'str',1:'1'}
	遍历params获取对应的value,然后根据参数排序的位置，获取args中对应的值放到map中
	并且还根据参数顺序存储了key为param1,param2的参数。
	假如我们调用上面方法传入的参数是：20  张三 ，则param中存储的就是{'str':'20','1':'张三','param1':'20','param2':'张三'}
	继续执行后面的内容和sqlSession方式的调用就一样了
	
###### sqlSession式方法调用

```
Map<String,Object> map = new HashMap<>(2);
map.put("ids",id);
map.put("name",name);
selectList = sqlSession.selectList("selectByParam", map);
```
	

```
public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
	try {
		//根据statement获取MappedStatement
		MappedStatement ms = configuration.getMappedStatement(statement);
		//执行
		List<E> result = executor.<E>query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
		return result;
	} catch (Exception e) {
		throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
	}
	finally {
		ErrorContext.instance().reset();
	}
}
```

```
private Object wrapCollection(final Object object) {
	if (object instanceof List) {
		StrictMap<Object> map = new StrictMap<Object>();
		map.put("list", object);
		return map;
	} else if (object != null && object.getClass().isArray()) {
		StrictMap<Object> map = new StrictMap<Object>();
		map.put("array", object);
		return map;
	}
	return object;
}
```
如果参数是集合或者数组则封装到map中,key分别对应为list或array,从而说明当我们dao层参数只有一个集合或数组时,<foreach>默认取list或者array。

```
public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
	BoundSql boundSql = ms.getBoundSql(parameterObject);
	CacheKey key = createCacheKey(ms, parameterObject, rowBounds, boundSql);
	return query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
}
```

```
public BoundSql getBoundSql(Object parameterObject) {
	BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
	List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
	if (parameterMappings == null || parameterMappings.size() <= 0) {
		boundSql = new BoundSql(configuration, boundSql.getSql(), parameterMap.getParameterMappings(), parameterObject);
	}

	// check for nested result maps in parameter mappings (issue #30)
	for (ParameterMapping pm : boundSql.getParameterMappings()) {
		String rmId = pm.getResultMapId();
		if (rmId != null) {
			ResultMap rm = configuration.getResultMap(rmId);
			if (rm != null) {
				hasNestedResultMaps |= rm.hasNestedResultMaps();
			}
		}
	}

	return boundSql;
}
```



```
public BoundSql getBoundSql(Object parameterObject) {
	DynamicContext context = new DynamicContext(configuration, parameterObject);
	rootSqlNode.apply(context);
	SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
	Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
	SqlSource sqlSource = sqlSourceParser.parse(context.getSql(), parameterType, context.getBindings());
	BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
	for (Map.Entry<String, Object> entry : context.getBindings().entrySet()) {
		boundSql.setAdditionalParameter(entry.getKey(), entry.getValue());
	}
	return boundSql;
}
```



```
public boolean apply(DynamicContext context) {
	GenericTokenParser parser = new GenericTokenParser("${", "}", new BindingTokenParser(context));
	context.appendSql(parser.parse(text));
	return true;
}
```


```
private static class BindingTokenParser implements TokenHandler {

	private DynamicContext context;

	public BindingTokenParser(DynamicContext context) {
		this.context = context;
	}

	public String handleToken(String content) {
		Object parameter = context.getBindings().get("_parameter");
		if (parameter == null) {
			context.getBindings().put("value", null);
		} else if (SimpleTypeRegistry.isSimpleType(parameter.getClass())) {
			context.getBindings().put("value", parameter);
		}
		Object value = OgnlCache.getValue(content, context.getBindings());
		return (value == null ? "" : String.valueOf(value)); // issue #274 return "" instead of "null"
	}
}
```
对于sql中${}形式的表达式，替换其内容


```
public SqlSource parse(String originalSql, Class<?> parameterType, Map<String, Object> additionalParameters) {
	ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler(configuration, parameterType, additionalParameters);
	GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
	String sql = parser.parse(originalSql);
	return new StaticSqlSource(configuration, sql, handler.getParameterMappings());
}
```

```
public String handleToken(String content) {
	parameterMappings.add(buildParameterMapping(content));
	return "?";
}
```
对于sql中#{}形式的表达式，替换为占位符形式的?



