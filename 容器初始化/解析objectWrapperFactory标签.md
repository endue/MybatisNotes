###### 进入解析objectWrapperFactory标签代码
```java
objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
```
```java
private void objectWrapperFactoryElement(XNode context) throws Exception {
	if (context != null) {
		//解析type属性值
		String type = context.getStringAttribute("type");
		//从BaseBuilder.typeAliasRegistry.TYPE_ALIASES中查找是否包含
		//当前key为type的类，包含返回，不包含则生成一个当前类的Class并返回
		ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).newInstance();
		//添加到BaseBuilder.configuration.objectWrapperFactory属性中
		configuration.setObjectWrapperFactory(factory);
	}
}
```
**此时我们自定义的所有对象包装工厂都保存到BaseBuilder.configuration.objectWrapperFactory中**

###### 对象包装工厂
对返回对象进行包装

###### 举例，查询出的book，默认name为:某*
自定义包装工厂
```java
public class MyObjectWrapperFactory implements ObjectWrapperFactory {
    @Override
    public boolean hasWrapperFor(Object object) {
        if(object instanceof Book){
            return true;
        }
        return false;
    }

    @Override
    public ObjectWrapper getWrapperFor(MetaObject metaObject, Object object) {
        return new MyObjectWrapper(metaObject,object);
    }
}
```
自定义包装类
```java
public class MyObjectWrapper extends BeanWrapper {

    private Object object;

    public MyObjectWrapper(MetaObject metaObject, Object object) {
        super(metaObject, object);
        this.object = object;
    }

    @Override
    public void set(PropertyTokenizer prop, Object value) {
        if(object instanceof Book){
            String name = prop.getName();
            if("name".equals(name)){
                value = new StringBuilder(((String) value).substring(0, 1)).append("*").toString();
            }
        }
        super.set(prop,value);
    }
}
```
配置文件
```xml
<objectWrapperFactory type="com.simon.objectWrapperFactory.MyObjectWrapperFactory"/>
```