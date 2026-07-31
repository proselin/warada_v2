package com.github.warada_v2.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.warada_v2.api.resources.rest.HealthCheckResource;
import org.junit.jupiter.api.Test;

class HealthCheckResourceTest {

  @Test
  void testPing() {
    HealthCheckResource resource = new HealthCheckResource();
    assertEquals("{\"status\":\"UP\"}", resource.health());
  }
}
