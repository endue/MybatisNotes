###### 进入解析databaseIdProvider标签代码
```java
databaseIdProviderElement(root.evalNode("databaseIdProvider"));
```
```java
private void databaseIdProviderElement(XNode context) throws Exception {
	DatabaseIdProvider databaseIdProvider = null;
	if (context != null) {
		//获取databaseIdProvider标签的type属性值
		String type = context.getStringAttribute("type");
		//获取子标签并封装成Properties
		Properties properties = context.getChildrenAsProperties();
		//获取databaseIdProvider,这个类需要实现org.apache.ibatis.mapping.DatabaseIdProvider
		databaseIdProvider = (DatabaseIdProvider) resolveClass(type).newInstance();
		//设置属性值
		databaseIdProvider.setProperties(properties);
	}
	//获取environment
	Environment environment = configuration.getEnvironment();
	if (environment != null && databaseIdProvider != null) {
		//根据environment中配置的DataSource获取DatabaseName，然后遍历上面配置文件中的配置信息
		//最后获取databaseId设置到BaseBuilder.configuration.databaseId中
		String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
		configuration.setDatabaseId(databaseId);
	}
}
```
**此时根据数据源获取的databaseId值为mysql,保存到BaseBuilder.configuration.databaseId中**