##### select执行
- 我们的方法走的是DefaultSqlSession中的selectOne:
```
public <T> T selectOne(String statement) {
	return this.<T>selectOne(statement, null);
}

public <T> T selectOne(String statement, Object parameter) {
	// Popular vote was to return null on 0 results and throw exception on too many.
	List<T> list = this.<T>selectList(statement, parameter);
	if (list.size() == 1) {
		return list.get(0);
	} else if (list.size() > 1) {
		throw new TooManyResultsException("Expected one result (or null) to be returned by selectOne(), but found: " + list.size());
	} else {
		return null;
	}
}
```
这里可以看出selectOne和selectList逻辑都是一致的，只不过selectOne会判断结果集是否只有一个

- 代码继续走this.selectList
```
public <E> List<E> selectList(String statement, Object parameter) {
	return this.<E>selectList(statement, parameter, RowBounds.DEFAULT);
}

public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
	try {
		MappedStatement ms = configuration.getMappedStatement(statement);
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
- 获取MappedStatement,然后执行executor.<E>query方法,executor对应的是CachingExecutor
```
public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
	//获取BoundSql
	BoundSql boundSql = ms.getBoundSql(parameterObject);
	//构建缓存key
	CacheKey key = createCacheKey(ms, parameterObject, rowBounds, boundSql);
	return query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
}
```
- 继续执行query方法
```
public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
	//获取并判断是否配置<Cache/>标签
	Cache cache = ms.getCache();
	if (cache != null) {
		//是否情况缓存
		flushCacheIfRequired(ms);
		//通过读写锁读取缓存
		if (ms.isUseCache() && resultHandler == null) {
			ensureNoOutParams(ms, parameterObject, boundSql);
			if (!dirty) {
				cache.getReadWriteLock().readLock().lock();
				try {
					@SuppressWarnings("unchecked")
					List<E> cachedList = (List<E>) cache.getObject(key);
					if (cachedList != null) return cachedList;
				}
				finally {
					cache.getReadWriteLock().readLock().unlock();
				}
			}
			List<E> list = delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
			tcm.putObject(cache, key, list); // issue #578. Query must be not synchronized to prevent deadlocks
			return list;
		}
	}
	return delegate.<E>query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
}
```
- 我们没有开启缓存，这里走最后一行代码delegate.<E>query,在BaseExecutor中
```
public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
	ErrorContext.instance().resource(ms.getResource()).activity("executing a query").object(ms.getId());
	if (closed) throw new ExecutorException("Executor was closed.");
	if (queryStack == 0 && ms.isFlushCacheRequired()) {
		clearLocalCache();
	}
	List<E> list;
	try {
		queryStack++;
		list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
		if (list != null) {
			handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
		} else {
			list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
		}
	} finally {
		queryStack--;
	}
	if (queryStack == 0) {
	for (DeferredLoad deferredLoad : deferredLoads) {
			deferredLoad.load();
		}
		deferredLoads.clear(); // issue #601
		if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
			clearLocalCache(); // issue #482
		}
	}
	return list;
}
```

- 继续走queryFromDatabase方法,依旧在BaseExecutor中
```
private <E> List<E> queryFromDatabase(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
	List<E> list;
	//一级缓存中放入占位符
	localCache.putObject(key, EXECUTION_PLACEHOLDER);
	try {
		list = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
	} finally {
		localCache.removeObject(key);
	}
	//将查询结果放入一级缓存
	localCache.putObject(key, list);
	if (ms.getStatementType() == StatementType.CALLABLE) {
		localOutputParameterCache.putObject(key, parameter);
	}
	return list;
}
```
这一步查询结果放入一级缓存,目的就是在同一个事务中,相同的调用同一个key时,结果可以从缓存中获取.详见FastResultSetHandler.getNestedQueryMappingValue方法
 this.executor.deferLoad(nestedQuery, metaResultObject, property, key, targetType);

- 继续走doQuery方法,在SimpleExecutor中
```
public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
	Statement stmt = null;
	try {
		Configuration configuration = ms.getConfiguration();
		StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, rowBounds, resultHandler, boundSql);
		stmt = prepareStatement(handler, ms.getStatementLog());
		return handler.<E>query(stmt, resultHandler);
	} finally {
		closeStatement(stmt);
	}
}
```

- 继续走handler.<E>query(stmt, resultHandler),跳转到PreparedStatementHandler中
```
public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
	PreparedStatement ps = (PreparedStatement) statement;
	ps.execute();
	return resultSetHandler.<E> handleResultSets(ps);
}
```

- 继续走resultSetHandler.<E> handleResultSets(ps)处理结果集,调整到FastResultSetHandler
```
public List<Object> handleResultSets(Statement stmt) throws SQLException {
	final List<Object> multipleResults = new ArrayList<Object>();
	final List<ResultMap> resultMaps = mappedStatement.getResultMaps();
	int resultMapCount = resultMaps.size();
	int resultSetCount = 0;
	ResultSet rs = stmt.getResultSet();

	while (rs == null) {
		// move forward to get the first resultset in case the driver
		// doesn't return the resultset as the first result (HSQLDB 2.1)
		if (stmt.getMoreResults()) {
			rs = stmt.getResultSet();
		} else {
			if (stmt.getUpdateCount() == -1) {
				// no more results.  Must be no resultset
				break;
			}
		}
	}

	validateResultMapsCount(rs, resultMapCount);
	while (rs != null && resultMapCount > resultSetCount) {
		final ResultMap resultMap = resultMaps.get(resultSetCount);
		ResultColumnCache resultColumnCache = new ResultColumnCache(rs.getMetaData(), configuration);
		handleResultSet(rs, resultMap, multipleResults, resultColumnCache);
		rs = getNextResultSet(stmt);
		cleanUpAfterHandlingResultSet();
		resultSetCount++;
	}
	return collapseSingleResultList(multipleResults);
}
```

- 继续走handleResultSet(rs, resultMap, multipleResults, resultColumnCache)
```
protected void handleResultSet(ResultSet rs, ResultMap resultMap, List<Object> multipleResults, ResultColumnCache resultColumnCache) throws SQLException {
	try {
		if (resultHandler == null) {
			DefaultResultHandler defaultResultHandler = new DefaultResultHandler(objectFactory);
			handleRowValues(rs, resultMap, defaultResultHandler, rowBounds, resultColumnCache);
			multipleResults.add(defaultResultHandler.getResultList());
		} else {
			handleRowValues(rs, resultMap, resultHandler, rowBounds, resultColumnCache);
		}
	} finally {
		closeResultSet(rs); // issue #228 (close resultsets)
	}
}
```

- 继续走handleRowValues(rs, resultMap, defaultResultHandler, rowBounds, resultColumnCache)
```
protected void handleRowValues(ResultSet rs, ResultMap resultMap, ResultHandler resultHandler, RowBounds rowBounds, ResultColumnCache resultColumnCache) throws SQLException {
	final DefaultResultContext resultContext = new DefaultResultContext();
	//判断是否需要分页,逻辑分页
	skipRows(rs, rowBounds);
	while (shouldProcessMoreRows(rs, resultContext, rowBounds)) {
		final ResultMap discriminatedResultMap = resolveDiscriminatedResultMap(rs, resultMap, null);
		Object rowValue = getRowValue(rs, discriminatedResultMap, null, resultColumnCache);
		callResultHandler(resultHandler, resultContext, rowValue);
	}
}
```

- 继续走getRowValue(rs, discriminatedResultMap, null, resultColumnCache)
```
protected Object getRowValue(ResultSet rs, ResultMap resultMap, CacheKey rowKey, ResultColumnCache resultColumnCache) throws SQLException {
	final ResultLoaderMap lazyLoader = instantiateResultLoaderMap();
	Object resultObject = createResultObject(rs, resultMap, lazyLoader, null, resultColumnCache);
	if (resultObject != null && !typeHandlerRegistry.hasTypeHandler(resultMap.getType())) {
		final MetaObject metaObject = configuration.newMetaObject(resultObject);
		boolean foundValues = resultMap.getConstructorResultMappings().size() > 0;
		//自动结果集映射
		//结果集中包含的column字段，但resultMap中没有的
		if (shouldApplyAutomaticMappings(resultMap, !AutoMappingBehavior.NONE.equals(configuration.getAutoMappingBehavior()))) {
			//获取未知的column
			final List<String> unmappedColumnNames = resultColumnCache.getUnmappedColumnNames(resultMap, null);
			//自动映射到metaObject中
			foundValues = applyAutomaticMappings(rs, unmappedColumnNames, metaObject, null, resultColumnCache) || foundValues;
		}
		//获取resultMap中包含的字段
		final List<String> mappedColumnNames = resultColumnCache.getMappedColumnNames(resultMap, null);
		//映射
		foundValues = applyPropertyMappings(rs, resultMap, mappedColumnNames, metaObject, lazyLoader, null) || foundValues;
		foundValues = (lazyLoader != null && lazyLoader.size() > 0) || foundValues;
		resultObject = foundValues ? resultObject : null;
		return resultObject;
	}
	return resultObject;
}
```
1. 映射结果集中包含的column但resultMap中不包含的字段
```
protected boolean applyAutomaticMappings(ResultSet rs, List<String> unmappedColumnNames, MetaObject metaObject, String columnPrefix, ResultColumnCache resultColumnCache) throws SQLException {
	boolean foundValues = false;
	for (String columnName : unmappedColumnNames) {
		String propertyName = columnName;
		if (columnPrefix != null && columnPrefix.length() > 0) {
			// When columnPrefix is specified,
			// ignore columns without the prefix.
			if (columnName.startsWith(columnPrefix)) {
				propertyName = columnName.substring(columnPrefix.length());
			} else {
				continue;
			}
		}
		//获取对象中的对应属性
		final String property = metaObject.findProperty(propertyName, configuration.isMapUnderscoreToCamelCase());
		//判断是否有此属性以及是否有对应的set方法
		if (property != null && metaObject.hasSetter(property)) {
			final Class<?> propertyType = metaObject.getSetterType(property);
			if (typeHandlerRegistry.hasTypeHandler(propertyType)) {
				final TypeHandler<?> typeHandler = resultColumnCache.getTypeHandler(propertyType, columnName);
				final Object value = typeHandler.getResult(rs, columnName);
				if (value != null || isCallSettersOnNulls(propertyType)) { // issue #377, call setter on nulls
					metaObject.setValue(property, value);
					foundValues = (value != null) || foundValues;
				}
			}
		}
	}
	return foundValues;
}
```
如果结果集中包含column但resultMap中没有，则判断返回对象中是否包含当前column对应的属性以及提供set方法,如果有就设置值


2. 根据resultMap映射结果集
```
protected boolean applyPropertyMappings(ResultSet rs, ResultMap resultMap, List<String> mappedColumnNames, MetaObject metaObject, ResultLoaderMap lazyLoader, String columnPrefix) throws SQLException {
	boolean foundValues = false;
	//获取resultMap中配置的所有property标签值
	final List<ResultMapping> propertyMappings = resultMap.getPropertyResultMappings();
	//遍历设置值
	for (ResultMapping propertyMapping : propertyMappings) {
		final String column = prependPrefix(propertyMapping.getColumn(), columnPrefix);
		if (propertyMapping.isCompositeResult() || (column != null && mappedColumnNames.contains(column.toUpperCase(Locale.ENGLISH)))) {
			Object value = getPropertyMappingValue(rs, metaObject, propertyMapping, lazyLoader, columnPrefix);
			final String property = propertyMapping.getProperty(); // issue #541 make property optional
			if (value != OMIT && property != null && (value != null || isCallSettersOnNulls(metaObject.getSetterType(property)))) { // issue #377, call setter on nulls
				metaObject.setValue(property, value);
				foundValues = (value != null) || foundValues;
			}
		}
	}
	return foundValues;
}
```
根据resultMap映射结果集,即通过property、column标签


