package com.demo.service;

import com.demo.dao.BookDao;
import com.demo.entity.Book;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * @Author: lim
 * @Description:
 * @Date: 2018/9/6.
 */
public class BookService implements BookDao {

    private static SqlSessionFactory sqlSessionFactory;
    private static Reader reader;

    static {
        try {
            reader = Resources.getResourceAsReader("mybatis-config.xml");
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Book> getAll() {
        SqlSession sqlSession = sqlSessionFactory.openSession();

        try {
           List<Book> list = sqlSession.selectList("bookNameSpace.getAll");
           return list;
        }finally {
           sqlSession.close();
        }
    }

    @Override
    public void insert(Book book) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            sqlSession.insert("bookNameSpace.insert", book);
            sqlSession.commit();
        }catch (Exception e){
            sqlSession.rollback();
        }finally {
            sqlSession.close();
        }
    }



    @Override
    public Book query() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        Object o = null;
        try {
            o = sqlSession.selectOne("bookNameSpace.query");
            sqlSession.commit();
        }catch (Exception e){
            sqlSession.rollback();
        }finally {
            sqlSession.close();
        }
        return (Book) o;
    }

    @Override
    public int count() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        Object o = null;
        try {
            o = sqlSession.selectOne("bookNameSpace.count");
            sqlSession.commit();
        }catch (Exception e){
            sqlSession.rollback();
        }finally {
            sqlSession.close();
        }
        return (int) o;
    }

    @Override
    public void insertAll(List<Book> books) {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            for(Book book : books){
                int id = sqlSession.insert("bookNameSpace.insert", book);
                System.out.println("book="+book);
            }
            sqlSession.commit();
        }catch (Exception e){
            sqlSession.rollback();
        }finally {
            sqlSession.close();
        }
    }
}
