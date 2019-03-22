###### 进入解析typeHandlers标签代码
```java
typeHandlerElement(root.evalNode("typeHandlers"));
```
```java
private void typeHandlerElement(XNode parent) throws Exception {
	if (parent != null) {
		//获取typeHandlers下的子标签：package或typeHandler
		for (XNode child : parent.getChildren()) {
			//如果是package，解析当前包以及子包下的类，并
			if ("package".equals(child.getName())) {
				//获取package标签name属性值
				String typeHandlerPackage = child.getStringAttribute("name");
				typeHandlerRegistry.register(typeHandlerPackage);
			} else {//否则就是typeHandler
				//获取typeHandler标签javaType、javaType、javaType属性值
				String javaTypeName = child.getStringAttribute("javaType");
				String jdbcTypeName = child.getStringAttribute("javaType");
				String handlerTypeName = child.getStringAttribute("javaType");
				//typeAliasRegistry中根据javaTypeName获取java类型
				//没有就创建一个
				Class<?> javaTypeClass = resolveClass(javaTypeName);
				//获取JdbcType数据库类型
				JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
				//typeAliasRegistry中根据handlerTypeName获取java类型处理器
				//没有就创建一个
				Class<?> typeHandlerClass = resolveClass(handlerTypeName);
				//注册到typeHandlerRegistry中
				if (javaTypeClass != null) {
					if (jdbcType == null) {
						//只有java类型和java类型处理器
						typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
					} else {
						////只有java类型、数据库类型和java类型处理器
						typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
					}
				} else {
					//只有类型处理器
					typeHandlerRegistry.register(typeHandlerClass);
				}
			}
		}
	}
}
```
###### typeHandlers标签下分为两种配置方式
```xml
 <typeHandlers>
        <typeHandler handler="com.simon.typehandler.MyTypeHandlers" jdbcType="VARCHAR" javaType="java.lang.String"/>
        <package name="com.simon.typehandler"></package>
 </typeHandlers>
```
1. package方式
```java
String typeHandlerPackage = child.getStringAttribute("name");
typeHandlerRegistry.register(typeHandlerPackage);
```
```java
public void register(String packageName) {
	ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
	resolverUtil.find(new ResolverUtil.IsA(TypeHandler.class), packageName);
	Set<Class<? extends Class<?>>> handlerSet = resolverUtil.getClasses();
	for (Class<?> type : handlerSet) {
		//Ignore inner classes and interfaces (including package-info.java) and abstract classes
		if (!type.isAnonymousClass() && !type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
			register(type);
		}
	}
}
```
创建一个resolverUtil，之后扫描packageName包下以及子包下的类，放到resolverUtil类的matches中。matches是个HashSet。
然后获取matches并遍历，获取里面的类信息。判断类是否为匿名类，是否为接口，是否为成员类。如果都不是，则调用register(type)方法。

```java
public void register(Class<?> typeHandlerClass) {
	boolean mappedTypeFound = false;
	//获取注解
	MappedTypes mappedTypes = typeHandlerClass.getAnnotation(MappedTypes.class);
	if (mappedTypes != null) {
		for (Class<?> javaTypeClass : mappedTypes.value()) {
			register(javaTypeClass, typeHandlerClass);
			mappedTypeFound = true;
		}
	}
	if (!mappedTypeFound) {
		register(getInstance(null, typeHandlerClass));
	}
}
```
- 注解不为null
```java
public void register(Class<?> javaTypeClass, Class<?> typeHandlerClass) {
	register(javaTypeClass, getInstance(javaTypeClass, typeHandlerClass));
}
```
```java
private <T> void register(Type javaType, TypeHandler<? extends T> typeHandler) {
	//获取MappedJdbcTypes注解
	MappedJdbcTypes mappedJdbcTypes = typeHandler.getClass().getAnnotation(MappedJdbcTypes.class);
	//判断是否有MappedJdbcTypes注解，有就带MappedJdbcTypes注册,没有就传null
	if (mappedJdbcTypes != null) {
		for (JdbcType handledJdbcType : mappedJdbcTypes.value()) {
			register(javaType, handledJdbcType, typeHandler);
		}
		if (mappedJdbcTypes.includeNullJdbcType()) {
			register(javaType, null, typeHandler);
		}
	} else {
		register(javaType, null, typeHandler);
	}
}
```
```java
private void register(Type javaType, JdbcType jdbcType, TypeHandler<?> handler) {
	//javaType不为null
	if (javaType != null) {
		//获取configuration.typeHandlerRegistry.TYPE_HANDLER_MAP，之后放入进去
		//TYPE_HANDLER_MAP结构为Map<Type, Map<JdbcType, TypeHandler<?>>>
		Map<JdbcType, TypeHandler<?>> map = TYPE_HANDLER_MAP.get(javaType);
		if (map == null) {
			map = new HashMap<JdbcType, TypeHandler<?>>();
			TYPE_HANDLER_MAP.put(javaType, map);
		}
		map.put(jdbcType, handler);
		//判断基础转换类中是否包含，如果包含就替换
		if (reversePrimitiveMap.containsKey(javaType)) {
			register(reversePrimitiveMap.get(javaType), jdbcType, handler);
		}
	}
	//最后还要放到configuration.typeHandlerRegistry.ALL_TYPE_HANDLERS_MAP
	//ALL_TYPE_HANDLERS_MAP结构为Map<Class<?>, TypeHandler<?>>
	ALL_TYPE_HANDLERS_MAP.put(handler.getClass(), handler);
}
```

- 注解为null
```java
public <T> void register(TypeHandler<T> typeHandler) {
	boolean mappedTypeFound = false;
	MappedTypes mappedTypes = typeHandler.getClass().getAnnotation(MappedTypes.class);
	if (mappedTypes != null) {
		for (Class<?> handledType : mappedTypes.value()) {
			register(handledType, typeHandler);
			mappedTypeFound = true;
		}
	}
	// @since 3.1.0 - try to auto-discover the mapped type
	if (!mappedTypeFound && typeHandler instanceof TypeReference) {
		try {
			TypeReference<T> typeReference = (TypeReference<T>) typeHandler;
			register(typeReference.getRawType(), typeHandler);
			mappedTypeFound = true;
		} catch (Throwable t) {
			// maybe users define the TypeReference with a different type and are not assignable, so just ignore it
		}
	}
	if (!mappedTypeFound) {
		register((Class<T>) null, typeHandler);
	}
}
```
最后走的也是注解不为null的方法

2. typeHandler方式
```java
	String javaTypeName = child.getStringAttribute("javaType");
	String jdbcTypeName = child.getStringAttribute("jdbcType");
	String handlerTypeName = child.getStringAttribute("handler");
	Class<?> javaTypeClass = resolveClass(javaTypeName);
	JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
	Class<?> typeHandlerClass = resolveClass(handlerTypeName);
	if (javaTypeClass != null) {
		if (jdbcType == null) {
			typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
		} else {
			typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
		}
	} else {
		typeHandlerRegistry.register(typeHandlerClass);
	}
```
套路和package一样

**此时我们自定义的类型已经加载到configuration.typeHandlerRegistry中**