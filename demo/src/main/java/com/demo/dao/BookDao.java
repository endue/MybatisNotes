package com.demo.dao;


import com.demo.entity.Book;

import java.util.List;

/**
 * @Author: lim
 * @Description:
 * @Date: 2018/9/27.
 */
public interface BookDao {

    List<Book> getAll();

    void insert(Book book);

    Book query();

    int count();

    void insertAll(List<Book> books);
}
