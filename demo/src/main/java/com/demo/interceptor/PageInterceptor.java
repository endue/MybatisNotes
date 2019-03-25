package com.demo.interceptor;

import com.demo.page.Page;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Properties;

/**
 * @Author lim
 * @Date 2019/3/25 15:35
 * @Description 参考：https://blog.csdn.net/zhaomin_g/article/details/81190016
 */
@Intercepts(@Signature(
            type = StatementHandler.class,
            method =  "prepare",
            args = {Connection.class}
))
public class PageInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        StatementHandler sonStatementHandler = (StatementHandler)getFieldValue(statementHandler,"delegate");

        RowBounds rowBounds = (RowBounds)getFieldValue(sonStatementHandler, "rowBounds");
        MappedStatement mappedStatement = (MappedStatement)getFieldValue(sonStatementHandler, "mappedStatement");

        if(rowBounds != null &&
                mappedStatement!=null &&
                SqlCommandType.SELECT.equals(mappedStatement.getSqlCommandType()) &&
                Page.class.isAssignableFrom(rowBounds.getClass())
                ){
            Page page = (Page) rowBounds;
            BoundSql boundSql = statementHandler.getBoundSql();
            if(boundSql != null){
                setFieldValue(boundSql,"sql",page.getLimitSQL(boundSql.getSql()));

                //setFieldValue(rowBounds,"offset",0);
            }
        }
        return invocation.proceed();
    }

    /**
     * 拦截StatementHandler类型,走上面的intercept方法
     * @param target
     * @return
     */
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target,this);
    }

    @Override
    public void setProperties(Properties properties) {

    }

    private Object getFieldValue(Object target, String fieldName) {
        try {
            Field field = getField(target, fieldName);
            if (field != null) {
                return field.get(target);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Field getField(Object target, String fieldName) {
        Field field = null;
        for (Class<?> clazz = target.getClass(); clazz != Object.class;clazz = clazz.getSuperclass()) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                // ignore
            }
        }
        if (field != null) {
            if (!field.isAccessible()) {
                try {
                    field.setAccessible(true);
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return field;
    }

    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            Field field = getField(target, fieldName);
            if (field != null) {
                field.set(target, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
