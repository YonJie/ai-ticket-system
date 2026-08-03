package com.aiticket.dto;

import java.util.List;

/**
 * 统一分页响应结构。
 *
 * @param <T> 列表元素类型
 */
public class PageResult<T> {

    private List<T> content;
    private long total;
    private int page;
    private int size;
    private int totalPages;

    public PageResult() {
    }

    /**
     * @param content    当前页数据
     * @param total      总条数
     * @param page       当前页（从 0 开始）
     * @param size       每页大小
     * @param totalPages 总页数
     */
    public PageResult(List<T> content, long total, int page, int size, int totalPages) {
        this.content = content;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
