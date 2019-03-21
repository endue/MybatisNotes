###### 进入解析settings标签代码
```java
settingsElement(root.evalNode("settings"));
```
```java
private void settingsElement(XNode context) throws Exception {
	if (context != null) {
		//获取settings标签下的子标签
		Properties props = context.getChildrenAsProperties();
		// Check that all settings are known to the configuration class
		//获取Configuration类的配置信息，然后校验配置文件中的配置是否在Configuration中存在对应的set方法
		MetaClass metaConfig = MetaClass.forClass(Configuration.class);
		for (Object key : props.keySet()) {
			if (!metaConfig.hasSetter(String.valueOf(key))) {
				throw new BuilderException("The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
			}
		}
		//重新设置Configuration中的默认值
		configuration.setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
		configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
		configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));
		configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
		configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), true));
		configuration.setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));
		configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
		configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
		configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
		configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
		configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
		configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));
		configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
		configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
		configuration.setLazyLoadTriggerMethods(stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
		configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
		configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
		configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
		configuration.setLogPrefix(props.getProperty("logPrefix"));
		configuration.setLogImpl(resolveClass(props.getProperty("logImpl")));
	}
}
```
**此时所有的settings下的配置都加载到BaseBuilder.configuration对应的属性中**