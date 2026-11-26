package com.networkmonitor.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * REST API 使用的通用分页响应。
 *
 * <p>Spring Data 的 Page 对象直接序列化时结构会随版本变化，
 * 用一个稳定的 DTO 可以让前端接口更可预测。
 *
 * @param <T> 列表元素类型
 */
public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;

    private PageResponse(List<T> content,
                         int page,
                         int size,
                         long totalElements,
                         int totalPages,
                         boolean first,
                         boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isLast() {
        return last;
    }
}
