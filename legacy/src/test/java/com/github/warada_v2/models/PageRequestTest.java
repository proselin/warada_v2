package com.github.warada_v2.models;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PageRequestTest {

    @Test
    void testToPageDefault() {
        PageRequest request = new PageRequest();
        Page page = request.toPage();
        assertNotNull(page);
        assertEquals(0, page.index);
        assertEquals(20, page.size);
    }

    @Test
    void testToPageCustom() {
        PageRequest request = new PageRequest(2, 50, "id", "desc");
        Page page = request.toPage();
        assertNotNull(page);
        assertEquals(2, page.index);
        assertEquals(50, page.size);
    }

    @Test
    void testGetOffset() {
        PageRequest request = new PageRequest(3, 15, "title", "asc");
        assertEquals(45, request.getOffSet());
    }

    @Test
    void testToSortNullOrBlank() {
        PageRequest request1 = new PageRequest();
        assertNull(request1.toSort());

        PageRequest request2 = new PageRequest();
        request2.setSortBy("   ");
        assertNull(request2.toSort());
    }

    @Test
    void testToSortAscending() {
        PageRequest request = new PageRequest();
        request.setSortBy("title");
        request.setSortOrder("asc");

        Sort sort = request.toSort();
        assertNotNull(sort);
        assertEquals(1, sort.getColumns().size());
        assertEquals("title", sort.getColumns().get(0).getName());
        assertEquals(Sort.Direction.Ascending, sort.getColumns().get(0).getDirection());
    }

    @Test
    void testToSortDescending() {
        PageRequest request = new PageRequest();
        request.setSortBy("createdAt");
        request.setSortOrder("DESC");

        Sort sort = request.toSort();
        assertNotNull(sort);
        assertEquals(1, sort.getColumns().size());
        assertEquals("createdAt", sort.getColumns().get(0).getName());
        assertEquals(Sort.Direction.Descending, sort.getColumns().get(0).getDirection());
    }

    @Test
    void testToSortUnknownOrder() {
        PageRequest request = new PageRequest();
        request.setSortBy("title");
        request.setSortOrder("unknown");

        assertNull(request.toSort());
    }
}
