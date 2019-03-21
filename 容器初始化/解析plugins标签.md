###### 进入解析plugins标签代码
```java
pluginElement(root.evalNode("plugins"));
```
```java
private void pluginElement(XNode parent) throws Exception {
	if (parent != null) {
		//遍历子标签
	for (XNode child : parent.getChildren()) {
			//获取子标签的interceptor属性值
			String interceptor = child.getStringAttribute("interceptor");
			//获取子标签配置的属性并返回一个properties
			Properties properties = child.getChildrenAsProperties();
			//从BaseBuilder.typeAliasRegistry.TYPE_ALIASES中查找是否包含
			//当前key为interceptor配置的类或者别名，包含返回，不包含则生成一个当前类的Class并返回
			Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).newInstance();
			//保存设置的属性值
			interceptorInstance.setProperties(properties);
			//添加到BaseBuilder.configuration.interceptorChain属性中
			//interceptorChain中有一个interceptors的ArrayList
			configuration.addInterceptor(interceptorInstance);
		}
	}
}
```
**此时所有的插件都配置到configuration.interceptorChain.interceptors中**