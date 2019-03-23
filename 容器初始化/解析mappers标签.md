##### 进入解析mappers标签代码
```java
mapperElement(root.evalNode("mappers"));
```
```java
private void mapperElement(XNode parent) throws Exception {
	if (parent != null) {
	for (XNode child : parent.getChildren()) {
			//如果为package标签
			if ("package".equals(child.getName())) {
				String mapperPackage = child.getStringAttribute("name");
				configuration.addMappers(mapperPackage);
			} else {
				//获取resource、url、class标签，三个标签只允许配置一个
				String resource = child.getStringAttribute("resource");
				String url = child.getStringAttribute("url");
				String mapperClass = child.getStringAttribute("class");
				if (resource != null && url == null && mapperClass == null) {
					ErrorContext.instance().resource(resource);
					InputStream inputStream = Resources.getResourceAsStream(resource);
					XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
					mapperParser.parse();
				} else if (resource == null && url != null && mapperClass == null) {
					ErrorContext.instance().resource(url);
					InputStream inputStream = Resources.getUrlAsStream(url);
					XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
					mapperParser.parse();
				} else if (resource == null && url == null && mapperClass != null) {
					Class<?> mapperInterface = Resources.classForName(mapperClass);
					configuration.addMapper(mapperInterface);
				} else {
					throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
				}
			}
		}
	}
}
```
- 这几中方式在配置文件中的大体配置如下
```xml
<!-- 使用相对于类路径的资源引用 -->
<mappers>
  <mapper resource="org/mybatis/builder/AuthorMapper.xml"/>
  <mapper resource="org/mybatis/builder/BlogMapper.xml"/>
  <mapper resource="org/mybatis/builder/PostMapper.xml"/>
</mappers>
<!-- 使用完全限定资源定位符（URL） -->
<mappers>
  <mapper url="file:///var/mappers/AuthorMapper.xml"/>
  <mapper url="file:///var/mappers/BlogMapper.xml"/>
  <mapper url="file:///var/mappers/PostMapper.xml"/>
</mappers>
<!-- 使用映射器接口实现类的完全限定类名 -->
<mappers>
  <mapper class="org.mybatis.builder.AuthorMapper"/>
  <mapper class="org.mybatis.builder.BlogMapper"/>
  <mapper class="org.mybatis.builder.PostMapper"/>
</mappers>
<!-- 将包内的映射器接口实现全部注册为映射器 -->
<mappers>
  <package name="org.mybatis.builder"/>
</mappers>
```
**注意：使用映射器接口实现类的完全限定类名，xml文件要和Mapper文件到同一目录下**

- 也可以同时使用mapper标签和package标签，但是需要先定义mapper标签在定义package标签
```xml
<mappers>
   <mapper resource="mybatis/book.xml"/>
   <package name="com.demo.dao.map"/>
</mappers>
```
**注意：同时使用两个标签的时候，要保证mapper文件中的namespace和package包下的类路径不要同名，
  否则会出现throw new BindingException("Type " + type + " is already known to the MapperRegistry.")的异常。**

###### package方式
```java
//获取package标签的name值
String mapperPackage = child.getStringAttribute("name");
//传入package路径
configuration.addMappers(mapperPackage);
```
调用configuration.mapperRegistry.addMappers方法
```java
public void addMappers(String packageName) {
    addMappers(packageName, Object.class);
}

public void addMappers(String packageName, Class<?> superType) {
	//获取当前路径下的.class文件
	ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
	resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
	Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
	for (Class<?> mapperClass : mapperSet) {
		addMapper(mapperClass);
	}
}
```
获取当前路径下的.class文件，遍历:
```java
public <T> void addMapper(Class<T> type) {
	//判断当前类是否是个接口
	if (type.isInterface()) {
		//判断configuration.mapperRegistry.knownMappers中是否包含当前key
		if (hasMapper(type)) {
			throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
		}
		boolean loadCompleted = false;
		try {
			//将当前类放到configuration.mapperRegistry.knownMappers中是
			knownMappers.put(type, new MapperProxyFactory<T>(type));
			// It's important that the type is added before the parser is run
			// otherwise the binding may automatically be attempted by the
			// mapper parser. If the type is already known, it won't try.
			MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
			parser.parse();
			loadCompleted = true;
		}
		finally {
			if (!loadCompleted) {
				knownMappers.remove(type);
			}
		}
	}
}
```
如果当前接口没有解析过，则将当前接口类放到configuration.mapperRegistry.knownMappers中，同时创建MapperAnnotationBuilder
调用parse()方法
```java
//每个接口都创建一个MapperAnnotationBuilder，然后调用MapperAnnotationBuilder.parse()方法
public void parse() {
	String resource = type.toString();
	//判断configuration.loadedResources是否包含当前接口类
	//包含说明解析过则跳过
	if (!configuration.isResourceLoaded(resource)) {
		//解析解析对应的xml文件
		loadXmlResource();
		//将当前接口类放入到configuration.loadedResources中
		configuration.addLoadedResource(resource);
		//设置MapperAnnotationBuilder.assistant.currentNamespace
		assistant.setCurrentNamespace(type.getName());
		//解析cache和cache-ref缓存标签
		parseCache();
		parseCacheRef();
		//获取当前接口类的方法
		Method[] methods = type.getMethods();
	for (Method method : methods) {
			try {
				//解析xml文件中的方法，然后和当前命名空间进行绑定
				parseStatement(method);
			} catch (IncompleteElementException e) {
				configuration.addIncompleteMethod(new MethodResolver(this, method));
			}
		}
	}
	parsePendingMethods();
}

private void loadXmlResource() {
	// Spring may not know the real resource name so we check a flag
	// to prevent loading again a resource twice
	// this flag is set at XMLMapperBuilder#bindMapperForNamespace
	//再次判断configuration.loadedResources中是否包含当前接口类
	//获取key为 如：namespace:com.demo.BookDao
	if (!configuration.isResourceLoaded("namespace:" + type.getName())) {
		//获取当前接口类的xml文件
		String xmlResource = type.getName().replace('.', '/') + ".xml";
		InputStream inputStream = null;
		try {
			//加载文件
			inputStream = Resources.getResourceAsStream(type.getClassLoader(), xmlResource);
		} catch (IOException e) {
			// ignore, resource is not required
		}
		if (inputStream != null) {
			//解析xml文件，并创建一个新的XMLMapperBuilder(初始化里面的一些属性)
			//并将当前接口类的全名赋值给XMLMapperBuilder.builderAssistant.currentNamespace
			XMLMapperBuilder xmlParser = new XMLMapperBuilder(inputStream, assistant.getConfiguration(), xmlResource, configuration.getSqlFragments(), type.getName());
			xmlParser.parse();
		}
	}
}



private void parseCache() {
	CacheNamespace cacheDomain = type.getAnnotation(CacheNamespace.class);
	if (cacheDomain != null) {
		assistant.useNewCache(cacheDomain.implementation(), cacheDomain.eviction(), cacheDomain.flushInterval(), cacheDomain.size(), cacheDomain.readWrite(), null);
	}
}
public Cache useNewCache(Class<? extends Cache> typeClass,
                         Class<? extends Cache> evictionClass,
                         Long flushInterval,
                         Integer size,
                         boolean readWrite,
                         Properties props) {
	typeClass = valueOrDefault(typeClass, PerpetualCache.class);
	evictionClass = valueOrDefault(evictionClass, LruCache.class);
	Cache cache = new CacheBuilder(currentNamespace)
	.implementation(typeClass)
	.addDecorator(evictionClass)
	.clearInterval(flushInterval)
	.size(size)
	.readWrite(readWrite)
	.properties(props)
	.build();
	//将cache放入到configuration.caches中
	configuration.addCache(cache);
	//同时指定当前xml文件的缓存
	currentCache = cache;
	return cache;
}


private void parseCacheRef() {
	CacheNamespaceRef cacheDomainRef = type.getAnnotation(CacheNamespaceRef.class);
	if (cacheDomainRef != null) {
		assistant.useCacheRef(cacheDomainRef.value().getName());
	}
}
public Cache useCacheRef(String namespace) {
	if (namespace == null) {
		throw new BuilderException("cache-ref element requires a namespace attribute.");
	}
	try {
		unresolvedCacheRef = true;
		Cache cache = configuration.getCache(namespace);
		if (cache == null) {
			throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.");
		}
		//指定当前xml文件的缓存
		currentCache = cache;
		unresolvedCacheRef = false;
		return cache;
	} catch (IllegalArgumentException e) {
		throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.", e);
	}
}
```

*****class方式同package方式。url和resource相同。所有的都方式都是想通的**

