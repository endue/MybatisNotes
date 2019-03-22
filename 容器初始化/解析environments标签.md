###### 进入解析environments标签代表
```java
environmentsElement(root.evalNode("environments"));
```
```java
private void environmentsElement(XNode context) throws Exception {
	if (context != null) {
		//判断environment是否为空
		if (environment == null) {
			//设置XMLConfigBuilder.environment值为environments标签default属性的值
			environment = context.getStringAttribute("default");
		}
		//遍历子标签
		for (XNode child : context.getChildren()) {
			//获取子标签ID
			String id = child.getStringAttribute("id");
			//加载子标签ID属性值和父标签的default值相同的配置项
			if (isSpecifiedEnvironment(id)) {
				//获取事务工厂
				TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
				//获取数据源工厂
				DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
				DataSource dataSource = dsFactory.getDataSource();
				//创建Environment
				Environment.Builder environmentBuilder = new Environment.Builder(id)
				.transactionFactory(txFactory)
				.dataSource(dataSource);
				//赋值到configuration.environment
				configuration.setEnvironment(environmentBuilder.build());
			}
		}
	}
}
```
首先判断了属性environment是否为空(这个是我们在创建XMLConfigBuilder类的时候，可以传进来的)。如果为空则获取标签environments
默认属性default的值。遍历子标签，获取子标签属性id的值。

###### 获取事务工厂
```java
TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
```
```java
private TransactionFactory transactionManagerElement(XNode context) throws Exception {
	if (context != null) {
		//获取transactionManager标签的type属性
		String type = context.getStringAttribute("type");
		//获取transactionManager标签的的子标签值
		Properties props = context.getChildrenAsProperties();
		//从BaseBuilder.typeAliasRegistry(同configuration.typeAliasRegistry)类中查找key是JDBC(配置文件为JDB)的类
		//mybatis内置了两个事务管理工厂JDBC和MANAGED，详见org.apache.ibatis.session.Configuration类的构造器
		TransactionFactory factory = (TransactionFactory) resolveClass(type).newInstance();
		factory.setProperties(props);
		return factory;
	}
	throw new BuilderException("Environment declaration requires a TransactionFactory.");
}
```
###### 获取数据源
```java
DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
```
```java
private DataSourceFactory dataSourceElement(XNode context) throws Exception {
	if (context != null) {
		String type = context.getStringAttribute("type");
		Properties props = context.getChildrenAsProperties();
		DataSourceFactory factory = (DataSourceFactory) resolveClass(type).newInstance();
		factory.setProperties(props);
		return factory;
	}
	throw new BuilderException("Environment declaration requires a DataSourceFactory.");
}
```
**此时事务工厂和数据源已经保存到BaseBuilder.configuration.environment中**
