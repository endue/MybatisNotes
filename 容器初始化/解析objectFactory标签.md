###### 进入解析objectFactory标签代码
```java
objectFactoryElement(root.evalNode("objectFactory"));
```
```java
private void objectFactoryElement(XNode context) throws Exception {
	if (context != null) {
		//解析type属性值
		String type = context.getStringAttribute("type");
		//获取子标签配置的属性并返回一个properties
		Properties properties = context.getChildrenAsProperties();
		//从BaseBuilder.typeAliasRegistry.TYPE_ALIASES中查找是否包含
		//当前key为type的类，包含返回，不包含则生成一个当前类的Class并返回
		ObjectFactory factory = (ObjectFactory) resolveClass(type).newInstance();
		//保存设置的属性值
		factory.setProperties(properties);
		//添加到BaseBuilder.configuration.objectFactory属性中
		configuration.setObjectFactory(factory);
	}
}
```
**此时我们自定义的所有对象工厂都被加载到BaseBuilder.configuration.objectFactory中**

####### 什么是对象工程
MyBatis 每次创建结果对象的新实例时，它都会使用一个对象工厂（ObjectFactory）实例来完成。 默认的对象工厂需要做的仅仅是实例化目标类，
要么通过默认构造方法，要么在参数映射存在的时候通过参数构造方法来实例化。 如果想覆盖对象工厂的默认行为，则可以通过创建自己的对象工厂来实现。

###### 举例：查询出的book，默认price为100
- 自定义对象工程
```java
public class MyObjectFactory extends DefaultObjectFactory {

    private Properties properties;

    /**
     * 通过默认构造方法创建对象
     * 默认构造方法也是通过调用有参的构造方法创建，只不过参数是null
     * @param type
     * @param <T>
     * @return
     */
    @Override
    public <T> T create(Class<T> type) {
        return super.create(type);
    }

    /**
     * 通过有参数的构造方法创建对象
     * @param type
     * @param constructorArgTypes
     * @param constructorArgs
     * @param <T>
     * @return
     */
    @Override
    public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
        T t = super.create(type, constructorArgTypes, constructorArgs);
        if(Book.class.isAssignableFrom(type)){
            ((Book)t).setPrice(100);
        }
        return t;
    }

    /**
     * 配置objectFactory标签中定义的属性
     * @param properties
     */
    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

}
```
- 配置文件
```xml
 <objectFactory type="com.simon.objectfactory.MyObjectFactory">
      <!--<property name="price" value="100"></property>-->
 </objectFactory>
```