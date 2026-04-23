package com.github.warada_v2.models;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResponseTest {

    @Test
    void testOfWithData() {
        List<String> content = List.of("A", "B", "C");
        PageResponse<String> response = PageResponse.of(content, 0, 5, 3);

        assertNotNull(response);
        assertEquals(3, response.getContent().size());
        assertEquals(0, response.getPage());
        assertEquals(5, response.getSize());
        assertEquals(3, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
        assertFalse(response.isEmpty());
    }

    @Test
    void testOfEmptyData() {
        PageResponse<String> response = PageResponse.of(List.of(), 0, 10, 0);

        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
        assertTrue(response.isEmpty());
    }

    @Test
    void testPaginationFlags() {
        // Page 1 out of 3 pages (indices 0, 1, 2). Size 5. Total 15.
        // For page=1, it's the middle page.
        PageResponse<String> response = PageResponse.of(List.of("A", "B", "C", "D", "E"), 1, 5, 15);

        assertFalse(response.isFirst());
        assertFalse(response.isLast());
        assertEquals(3, response.getTotalPages());
    }

    @Test
    void testGettersAndSetters() {
        PageResponse<Integer> response = new PageResponse<>();
        response.setContent(List.of(1, 2));
        response.setPage(1);
        response.setSize(10);
        response.setTotalElements(12);
        response.setTotalPages(2);
        response.setFirst(false);
        response.setLast(true);
        response.setEmpty(false);

        assertEquals(2, response.getContent().size());
        assertEquals(1, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(12, response.getTotalElements());
        assertEquals(2, response.getTotalPages());
        assertFalse(response.isFirst());
        assertTrue(response.isLast());
        assertFalse(response.isEmpty());
    }
}
