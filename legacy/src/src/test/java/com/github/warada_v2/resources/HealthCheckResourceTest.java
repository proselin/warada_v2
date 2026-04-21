package com.github.warada_v2.resources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthCheckResourceTest {

    @Test
    void testPing() {
        HealthCheckResource resource = new HealthCheckResource();
        assertEquals("pong", resource.ping());
    }
}
