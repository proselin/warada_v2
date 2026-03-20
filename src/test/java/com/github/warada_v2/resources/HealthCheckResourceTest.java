package com.github.warada_v2.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class HealthCheckResourceTest {

    @Test
    void testPing() {
        HealthCheckResource resource = new HealthCheckResource();
        String result = resource.ping().await().indefinitely();
        assertEquals("pong", result);
    }
}
