package com.demo.page;

import org.apache.ibatis.session.RowBounds;

/**
 * @Author lim
 * @Date 2019/3/25 17:41
 * @Description
 */
public class Page extends RowBounds{

    private int pageNo = 1;//页码，默认是第一页
    private int pageSize = 10;//每页显示的记录数，默认是15


    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public int getOffset() {
        return RowBounds.NO_ROW_OFFSET;
    }

    @Override
    public int getLimit() {
        return RowBounds.NO_ROW_LIMIT;
    }

    public String getLimitSQL(String sql) {
        sql = sql.trim();
        StringBuffer newSql = new StringBuffer(sql.length() + 50);
        newSql.append(sql).append(" limit ").append((pageNo - 1)* pageSize).append(",").append(pageSize);
        return newSql.toString();
    }
}
