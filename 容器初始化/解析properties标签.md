###### 进入解析properties标签代码
```java
propertiesElement(root.evalNode("properties"));
```
```java
private void propertiesElement(XNode context) throws Exception {
	if (context != null) {
		//获取properties标签下的property子标签并解析内容返回Properties对象
		Properties defaults = context.getChildrenAsProperties();
		//获取properties标签的resource属性值
		String resource = context.getStringAttribute("resource");
		//获取properties标签的url属性值
		String url = context.getStringAttribute("url");
		//如果url和resource都不为空则抛出异常
		if (resource != null && url != null) {
			throw new BuilderException("The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
		}
		//解析url或resource指定的Properties文件，将结果封装成Properties对象返回，放到defaults中
		if (resource != null) {
			defaults.putAll(Resources.getResourceAsProperties(resource));
		} else if (url != null) {
			defaults.putAll(Resources.getUrlAsProperties(url));
		}
		//获取父类configuration中的variables属性，不为空则将里面的值添加到defaults中
		//variables在SqlSessionFactoryBuilder.build中可以传入，我们传入的为null
		Properties vars = configuration.getVariables();
		if (vars != null) {
			defaults.putAll(vars);
		}
		//将配置文件中解析的properties标签内容放到configuration中的variables属性
		//以及XMLConfigBuilder的parser属性中
		parser.setVariables(defaults);
		configuration.setVariables(defaults);
	}
}
```
**此时properties文件中的内容已经加载到BaseBuilder.configuration.variables属性和
XMLConfigBuilder.parser.variables属性中**

