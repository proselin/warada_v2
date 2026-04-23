package com.github.warada_v2.models;

import java.util.List;
import java.util.Objects;

public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;

    public PageResponse() {
    }

    public PageResponse(List<T> content, int page, int size, long totalElements, int totalPages, boolean first,
                        boolean last, boolean empty) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
        this.empty = empty;
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
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

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean first = page == 0;
        boolean last = page >= totalPages - 1;
        boolean empty = content.isEmpty();

        return new PageResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages,
                first,
                last,
                empty
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PageResponse<?> that)) {
            return false;
        }
        return page == that.page
                && size == that.size
                && totalElements == that.totalElements
                && totalPages == that.totalPages
                && first == that.first
                && last == that.last
                && empty == that.empty
                && Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, page, size, totalElements, totalPages, first, last, empty);
    }

    @Override
    public String toString() {
        return "PageResponse{" +
                "content=" + content +
                ", page=" + page +
                ", size=" + size +
                ", totalElements=" + totalElements +
                ", totalPages=" + totalPages +
                ", first=" + first +
                ", last=" + last +
                ", empty=" + empty +
                '}';
    }
}