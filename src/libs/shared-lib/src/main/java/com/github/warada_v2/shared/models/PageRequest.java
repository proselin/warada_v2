package com.github.warada_v2.shared.models;

import java.util.Objects;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

public class PageRequest {
    private int page = 0;        // Page number (0-based)
    private int size = 20;       // Items per page
    private String sortBy;       // Field to sort by
    private String sortOrder = "asc";  // 'asc' or 'desc'

    public PageRequest() {
    }

    public PageRequest(int page, int size, String sortBy, String sortOrder) {
        this.page = page;
        this.size = size;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
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

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int getOffSet() {
        return page * size;
    }

    public Page toPage() {
        return Page.of(page, size);
    }

    public Sort toSort() {
        if (sortBy == null || sortBy.isBlank()) {
            return null;
        }
        return switch (sortOrder) {
            case "asc", "ASC" -> Sort.ascending(sortBy);
            case "desc", "DESC" -> Sort.descending(sortBy);
            default -> null;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PageRequest that)) {
            return false;
        }
        return page == that.page
                && size == that.size
                && Objects.equals(sortBy, that.sortBy)
                && Objects.equals(sortOrder, that.sortOrder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, size, sortBy, sortOrder);
    }

    @Override
    public String toString() {
        return "PageRequest{" +
                "page=" + page +
                ", size=" + size +
                ", sortBy='" + sortBy + '\'' +
                ", sortOrder='" + sortOrder + '\'' +
                '}';
    }
}
