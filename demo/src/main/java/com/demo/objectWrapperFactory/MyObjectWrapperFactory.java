package com.demo.objectWrapperFactory;

import com.demo.entity.Book;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;

/**
 * @Author lim
 * @Date 2019/3/22 13:39
 * @Description
 */
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
