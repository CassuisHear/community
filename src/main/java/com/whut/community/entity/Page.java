package com.whut.community.entity;

import lombok.NoArgsConstructor;

/**
 * 封装分页相关的信息
 */

@NoArgsConstructor
public class Page {

    //当前页码
    private int current = 1;

    //每一页显示的上限
    private int limit = 10;

    //limit 的上限值，也就是说每一页不能显示过多数据
    private static final int PAGE_LIMIT = 100;

    //数据总数(用于计算总页数)
    private int rows;

    //查询路径(用于复用分页链接)
    private String path;

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        //获取总页码
        int maxPage = getTotal();

        //当前页码的范围是 [1, maxPage]，这个范围内的赋值才会有效
        if (current >= 1 && current <= maxPage) {
            this.current = current;
        }
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        //设置的每页显示上限在范围 [1, PAGE_LIMIT] 内时才会有效
        if (limit >= 1 && limit <= PAGE_LIMIT) {
            this.limit = limit;
        }
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        if (rows >= 0) {
            this.rows = rows;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * 获取当前页的起始行
     *
     * @return 当前页的起始行
     */
    public int getOffset() {
        return (current - 1) * limit;
    }

    /**
     * 获取总页数
     *
     * @return 总页数
     */
    public int getTotal() {
        int ans = rows / limit;
        return rows % limit == 0 ? ans : ans + 1;
    }

    /*
        分页条不会显示所有的页码，
        应该根据当前页码 current 显示 包括 current 在内的前后几个页码，
        比如这里显示 [current - 2, current + 2] 这个范围内的页码，
        其中左边界为 from，右边界为 to
     */

    /**
     * 获取左边界 form
     *
     * @return 左边界 form
     */
    public int getFrom() {
        int from = current - 2;
        return Math.max(from, 1);
    }

    /**
     * 获取右边界 to
     *
     * @return 右边界 to
     */
    public int getTo() {
        int to = current + 2, maxPage = getTotal();
        return Math.min(to, maxPage);
    }
}
