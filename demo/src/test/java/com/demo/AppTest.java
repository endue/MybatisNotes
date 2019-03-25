package com.demo;

import com.demo.dao.BookDao;
import com.demo.entity.Book;
import com.demo.page.Page;
import com.demo.service.BookService;
import org.apache.ibatis.session.RowBounds;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for simple App.
 */
public class AppTest {

    private BookDao bookDao = new BookService();

    @Test
    public void getAllTest(){
        List<Book> list = bookDao.getAll();
        System.out.println(list);
    }

    @Test
    public void insertTest(){
        for(int i = 0;i<500;i++){
            Book b = new Book();
            b.setName("自然"+i);
            bookDao.insert(b);
        }
    }

    @Test
    public void selectTest(){
        System.out.println(bookDao.query());
    }

    @Test
    public void countTest(){
        System.out.println(bookDao.count());
    }

    @Test
    public void insertAllTest(){
        List<Book> list = new ArrayList<>();
        for(int i = 1;i<10;i++){
            Book b = new Book();
            b.setName("自然"+i);
            list.add(b);
        }
        bookDao.insertAll(list);
    }

    @Test
    public void queryForPageTest(){
        Page page = new Page();
        page.setPageSize(10);
        for(int i = 1;i<11;i++){
            page.setPageNo(i);
            List<Book> list = bookDao.queryForPage(page);
            System.out.println(list);
        }
    }
}
