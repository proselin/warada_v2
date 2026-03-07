package com.github.warada_v2.models;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
    private int page = 0;        // Page number (0-based)
    private int size = 20;       // Items per page
    private String sortBy;       // Field to sort by
    private String sortOrder = "asc";  // 'asc' or 'desc'

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
}