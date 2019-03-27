###### 开启session
```
SqlSession sqlSession = sqlSessionFactory.openSession();
```
- 查看如何获取session
```
public SqlSession openSession() {
	return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
}
```
```
private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
	Transaction tx = null;
	try {
		//获取环境配置
		final Environment environment = configuration.getEnvironment();
		//获取事务工厂
		final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
		//获取事务
		tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
		//获取执行器
		final Executor executor = configuration.newExecutor(tx, execType, autoCommit);
		return new DefaultSqlSession(configuration, executor);
	} catch (Exception e) {
		closeTransaction(tx); // may have fetched a connection so lets call close()
		throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
	}
	finally {
		ErrorContext.instance().reset();
	}
}
```
1. 如何获取执行器
```
public Executor newExecutor(Transaction transaction, ExecutorType executorType, boolean autoCommit) {
	//execType默认是ExecutorType.SIMPLE
	executorType = executorType == null ? defaultExecutorType : executorType;
	executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
	Executor executor;
	if (ExecutorType.BATCH == executorType) {
		executor = new BatchExecutor(this, transaction);
	} else if (ExecutorType.REUSE == executorType) {
		executor = new ReuseExecutor(this, transaction);
	} else {
		//我们的代码是走的这里
		executor = new SimpleExecutor(this, transaction);
	}
	//判断是否开启缓存，如果开启缓存则创建CachingExecutor，默认是true
	//这里用了装饰者模式
	if (cacheEnabled) {
		executor = new CachingExecutor(executor, autoCommit);
	}
	executor = (Executor) interceptorChain.pluginAll(executor);
	return executor;
}
```

2. 获取简单执行器的代码
```
public SimpleExecutor(Configuration configuration, Transaction transaction) {
	super(configuration, transaction);
}
```
```
protected BaseExecutor(Configuration configuration, Transaction transaction) {
	this.transaction = transaction;
	this.deferredLoads = new ConcurrentLinkedQueue<DeferredLoad>();
	this.localCache = new PerpetualCache("LocalCache");
	this.localOutputParameterCache = new PerpetualCache("LocalOutputParameterCache");
	this.closed = false;
	this.configuration = configuration;
}
```

###### Insert执行
```
sqlSession.insert("bookNameSpace.insert", book);
```
```
public int insert(String statement, Object parameter) {
	return update(statement, parameter);
}
```
```
public int update(String statement, Object parameter) {
	try {
		dirty = true;
		//获取MappedStatement
		MappedStatement ms = configuration.getMappedStatement(statement);
		return executor.update(ms, wrapCollection(parameter));
	} catch (Exception e) {
		throw ExceptionFactory.wrapException("Error updating database.  Cause: " + e, e);
	}
	finally {
		ErrorContext.instance().reset();
	}
}
```

1. 包装集合参数
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
这也就说明当我们参数只有list或者数组时，xml文件用foreach标签解析时collection必须是list或者array

2. 执行executor.update(executor是CachingExecutor)
```
public int update(MappedStatement ms, Object parameterObject) throws SQLException {
	//判断xml中是否配置了<cache/>标签即二级缓存以及对应的insert、update和delete语句开启了flushCache="true"
	//只要语句被调用，都会导致本地缓存和二级缓存被清空，默认值：true
	flushCacheIfRequired(ms);
	//delegate就是SimpleExecutor这里调用其父类BaseExecutor的update方法
	return delegate.update(ms, parameterObject);
}
```
- 看一下flushCacheIfRequired(ms)
```
private void flushCacheIfRequired(MappedStatement ms) {
	Cache cache = ms.getCache();
	if (cache != null && ms.isFlushCacheRequired()) {
		dirty = true; // issue #524. Disable using cached data for this session
		tcm.clear(cache);
	}
}
```
3. 执行delegate.update(ms, parameterObject)
```
public int update(MappedStatement ms, Object parameter) throws SQLException {
	ErrorContext.instance().resource(ms.getResource()).activity("executing an update").object(ms.getId());
	if (closed) throw new ExecutorException("Executor was closed.");
	//清空一级缓存
	clearLocalCache();
	return doUpdate(ms, parameter);
}
```
- 看一下清空一级缓存的代码
```
public void clearLocalCache() {
	if (!closed) {
		localCache.clear();
		localOutputParameterCache.clear();
	}
}

public void clearLocalCache() {
	if (!closed) {
		localCache.clear();
		localOutputParameterCache.clear();
	}
}

localCache和localOutputParameterCache都是PerpetualCache类
看一下PerpetualCache结构
public class PerpetualCache implements Cache {

	private String id;

	private Map<Object, Object> cache = new HashMap<Object, Object>();

	private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	
	...
}
```
清空一级缓存最后就是调用HashMap的clear方法

4. 执行doUpdate(ms, parameter)方法
```
public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
	Statement stmt = null;
	try {
		//获取MappedStatement中的configuration
		Configuration configuration = ms.getConfiguration();
		//获取StatementHandler
		StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
		stmt = prepareStatement(handler, ms.getStatementLog());
		return handler.update(stmt);
	} finally {
		closeStatement(stmt);
	}
}
```
- 看一下configuration.newStatementHandler(...)的代码
```
public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
	//创建StatementHandler
	StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
	//获取配置的插件,判断statementHandler中的方法是否在插件中别拦截
	//如果有则返回代理类，没有则返回原类
	statementHandler = (StatementHandler) interceptorChain.pluginAll(statementHandler);
	return statementHandler;
}
```

- 看一下new RoutingStatementHandler(...)的代码
```
public RoutingStatementHandler(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {

	switch (ms.getStatementType()) {
	case STATEMENT:
		delegate = new SimpleStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
		break;
	case PREPARED:
		//我们构建的是PreparedStatementHandler
		delegate = new PreparedStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
		break;
	case CALLABLE:
		delegate = new CallableStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
		break;
	default:
		throw new ExecutorException("Unknown statement type: " + ms.getStatementType());
	}

}
```

- 看一下new PreparedStatementHandler(...)
```
public PreparedStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
	super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
}
protected BaseStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
	this.configuration = mappedStatement.getConfiguration();
	this.executor = executor;
	this.mappedStatement = mappedStatement;
	this.rowBounds = rowBounds;

	this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
	this.objectFactory = configuration.getObjectFactory();

	if (boundSql == null) { // issue #435, get the key before calculating the statement
		generateKeys(parameterObject);
		boundSql = mappedStatement.getBoundSql(parameterObject);
	}

	this.boundSql = boundSql;

	this.parameterHandler = configuration.newParameterHandler(mappedStatement, parameterObject, boundSql);
	this.resultSetHandler = configuration.newResultSetHandler(executor, mappedStatement, rowBounds, parameterHandler, resultHandler, boundSql);
}
```

- 看一下prepareStatement(...)的代码
```
private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
	Statement stmt;
	//获取连接
	//如果日志级别是debug模式则创建连接的代理类
	Connection connection = getConnection(statementLog);
	//通过创建的StatementHandler初始化连接
	stmt = handler.prepare(connection);
	handler.parameterize(stmt);
	return stmt;
}
```

5. 执行handler.update(stmt);方法
```
public int update(Statement statement) throws SQLException {
	PreparedStatement ps = (PreparedStatement) statement;
	ps.execute();
	//获取影响的行数
	int rows = ps.getUpdateCount();
	//获取参数对象
	Object parameterObject = boundSql.getParameterObject();
	//获取主键生成器
	KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
	//后置处理
	keyGenerator.processAfter(executor, mappedStatement, ps, parameterObject);
	return rows;
}

public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
	//将参数对象放到list中
	List<Object> parameters = new ArrayList<Object>();
	parameters.add(parameter);
	//设置主键到对象中
	processBatch(ms, stmt, parameters);
}

public void processBatch(MappedStatement ms, Statement stmt, List<Object> parameters) {
	ResultSet rs = null;
	try {
		rs = stmt.getGeneratedKeys();
		final Configuration configuration = ms.getConfiguration();
		final TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
		final String[] keyProperties = ms.getKeyProperties();
		final ResultSetMetaData rsmd = rs.getMetaData();
		TypeHandler<?>[] typeHandlers = null;
		if (keyProperties != null && rsmd.getColumnCount() >= keyProperties.length) {
			for (Object parameter : parameters) {
				if (!rs.next()) break; // there should be one row for each statement (also one for each parameter)
				final MetaObject metaParam = configuration.newMetaObject(parameter);
				if (typeHandlers == null) typeHandlers = getTypeHandlers(typeHandlerRegistry, metaParam, keyProperties);
				populateKeys(rs, metaParam, keyProperties, typeHandlers);
			}
		}
	} catch (Exception e) {
		throw new ExecutorException("Error getting generated key or setting result to parameter object. Cause: " + e, e);
	}
	finally {
		if (rs != null) {
			try {
				rs.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}
}
```