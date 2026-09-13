package com.github.warada_v2.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.github.warada_v2.shared.exception_mappers.GlobalExceptionMapper;
import com.github.warada_v2.shared.models.ApiError;
import jakarta.ws.rs.NotFoundException;
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

  @Test
  void testNotFoundExceptionPassesThroughAs404() {
    GlobalExceptionMapper mapper = new GlobalExceptionMapper();
    NotFoundException ex = new NotFoundException();

    Response response = mapper.toResponse(ex);

    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    assertSame(ex.getResponse(), response);
  }
}
