package com.github.warada_v2.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiErrorTest {

    @Test
    void testApiErrorConstructor() {
        ApiError error = new ApiError("123-abc", 500, "Internal Server Error");

        assertEquals("123-abc", error.processId);
        assertEquals(500, error.status);
        assertEquals("Internal Server Error", error.message);
        assertNotNull(error.timestamp);
    }
}
