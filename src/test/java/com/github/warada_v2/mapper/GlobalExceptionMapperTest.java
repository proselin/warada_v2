package com.github.warada_v2.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.warada_v2.shared.exception_mappers.GlobalExceptionMapper;
import com.github.warada_v2.shared.models.ApiError;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

class GlobalExceptionMapperTest {

  @Test
  void testToResponse() {
    GlobalExceptionMapper mapper = new GlobalExceptionMapper();
    Throwable ex = new RuntimeException("Test exception");
    Response response = mapper.toResponse(ex);

    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    ApiError error = (ApiError) response.getEntity();
    assertNotNull(error.processId);
    assertEquals(500, error.status);
    assertEquals("Internal server error", error.message);
  }
}
