###### 进入解析typeAliases标签代码
```java
typeAliasesElement(root.evalNode("typeAliases"));
```
```java
private void typeAliasesElement(XNode parent) {
	if (parent != null) {
		//获取typeAliases标签下的子节点：typeAlias或者package
		for (XNode child : parent.getChildren()) {
			//如果定义的是package标签
			if ("package".equals(child.getName())) {
				String typeAliasPackage = child.getStringAttribute("name");
				//解析结果放到BaseBuilder.configuration.typeAliasRegistry属性
				configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
			} else {//否则即typeAlias标签
				String alias = child.getStringAttribute("alias");//获取alias标签值
				String type = child.getStringAttribute("type");//获取type标签值
				try {
					Class<?> clazz = Resources.classForName(type);
					if (alias == null) {//alias标签为空
						//解析结果放到BaseBuilder.typeAliasRegistry属性
						//BaseBuilder.typeAliasRegistry属性被final修饰,指向BaseBuilder.configuration.typeAliasRegistry
						typeAliasRegistry.registerAlias(clazz);
					} else {//alias标签非空
						//解析结果放到BaseBuilder.typeAliasRegistry属性
						//BaseBuilder.typeAliasRegistry属性被final修饰,指向BaseBuilder.configuration.typeAliasRegistry
						typeAliasRegistry.registerAlias(alias, clazz);
					}
				} catch (ClassNotFoundException e) {
					throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
				}
			}
		}
	}
}
```
**此时所有的typeAliases下的配置都加载到BaseBuilder.configuration.typeAliasRegistry属性中**

###### typeAliases标签下两种配置方式分析
```xml
<typeAliases>
     <!--<typeAlias type="com.simon.entity.Book" alias="book"/>-->
     <package name="com.simon.entity"></package>
</typeAliases>
```
1. package标签
```java
String typeAliasPackage = child.getStringAttribute("name");
configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
```
获取configuration.typeAliasRegistry属性，然后调用registerAliases(...)方法
```java
public void registerAliases(String packageName) {
	registerAliases(packageName, Object.class);
}

public void registerAliases(String packageName, Class<?> superType) {
	ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
	//解析当前包以及子包下的类
	resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
	Set<Class<? extends Class<?>>> typeSet = resolverUtil.getClasses();
	for(Class<?> type : typeSet) {
		// Ignore inner classes and interfaces (including package-info.java)
		// Skip also inner classes. See issue #6
		if (!type.isAnonymousClass() && !type.isInterface() && !type.isMemberClass()) {
			registerAlias(type);
		}
	}
}
```
创建一个resolverUtil，之后扫描packageName包下以及子包下的类，放到resolverUtil类的matches中。matches是个HashSet。
然后获取matches并遍历，获取里面的类信息。判断类是否为匿名类，是否为接口，是否为成员类。如果都不是，则调用registerAlias(type)方法。
```java
public void registerAlias(Class<?> type) {
	String alias = type.getSimpleName();
	//获取当前类的Alias注解
	Alias aliasAnnotation = type.getAnnotation(Alias.class);
	if (aliasAnnotation != null) {//如果注解不为空则alias为注解的value
		alias = aliasAnnotation.value();
	}
	//将别名、Class类传入方法
	registerAlias(alias, type);
}

public void registerAlias(String alias, Class<?> value) {
	if (alias == null) throw new TypeException("The parameter alias cannot be null");
	//将别名alias转为小写
	String key = alias.toLowerCase(Locale.ENGLISH); // issue #748
	//然后判断configuration.typeAliasRegistry.TYPE_ALIASES属性中是否包含这个别名
	if (TYPE_ALIASES.containsKey(key) && TYPE_ALIASES.get(key) != null && !TYPE_ALIASES.get(key).equals(value)) {
		throw new TypeException("The alias '" + alias + "' is already mapped to the value '" + TYPE_ALIASES.get(key).getName() + "'.");
	}
	TYPE_ALIASES.put(key, value);
}
```
2. typeAlias标签
```java
String alias = child.getStringAttribute("alias");
String type = child.getStringAttribute("type");
try {
	Class<?> clazz = Resources.classForName(type);
	if (alias == null) {
		typeAliasRegistry.registerAlias(clazz);
	} else {
		typeAliasRegistry.registerAlias(alias, clazz);
	}
} catch (ClassNotFoundException e) {
	throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
}
```
```java
 public void registerAlias(Class<?> type) {
    String alias = type.getSimpleName();
    Alias aliasAnnotation = type.getAnnotation(Alias.class);
    if (aliasAnnotation != null) {
      alias = aliasAnnotation.value();
    } 
    registerAlias(alias, type);
  }

  public void registerAlias(String alias, Class<?> value) {
    if (alias == null) throw new TypeException("The parameter alias cannot be null");
    String key = alias.toLowerCase(Locale.ENGLISH); // issue #748
    if (TYPE_ALIASES.containsKey(key) && TYPE_ALIASES.get(key) != null && !TYPE_ALIASES.get(key).equals(value)) {
      throw new TypeException("The alias '" + alias + "' is already mapped to the value '" + TYPE_ALIASES.get(key).getName() + "'.");
    }
    TYPE_ALIASES.put(key, value);
  }
```

**此时配置的typeAliases标签下的内容，已经加载到configuration.typeAliasRegistry.TYPE_ALIASES中。至此，我们可以在mybatis的xml中
可以不使用全路径包名来指定某个类，可以直接使用别名**