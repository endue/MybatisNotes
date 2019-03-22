package com.demo.objectWrapperFactory;

import com.demo.entity.Book;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.reflection.wrapper.BeanWrapper;

/**
 * @Author lim
 * @Date 2019/3/22 13:50
 * @Description
 */
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
