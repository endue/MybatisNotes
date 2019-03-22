package com.demo.objectfactory;

import com.demo.entity.Book;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;

import java.util.List;
import java.util.Properties;

/**
 * @Author lim
 * @Date 2019/3/22 10:05
 * @Description
 */
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
