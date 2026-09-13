package com.github.warada_v2.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.warada_v2.shared.exception_mappers.ConflictExceptionMapper;
import com.github.warada_v2.shared.exceptions.BusinessException;
import com.github.warada_v2.shared.exceptions.ConflictException;
import com.github.warada_v2.shared.exceptions.ResourceNotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

class ExceptionsTest {

  @Test
  void testConflictExceptionMapper() {
    ConflictExceptionMapper mapper = new ConflictExceptionMapper();
    ConflictException exception = new ConflictException("Conflict occurred");
    Response response = mapper.toResponse(exception);

    assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
    assertEquals("Conflict occurred", response.getEntity());
  }

  @Test
  void testResourceNotFoundException() {
    ResourceNotFoundException ex1 = new ResourceNotFoundException("Not found");
    assertEquals("Not found", ex1.getMessage());

    ResourceNotFoundException ex2 = new ResourceNotFoundException("User", "id", 1);
    assertEquals("User not found with id: 1", ex2.getMessage());
  }

  @Test
  void testBusinessException() {
    BusinessException ex1 = new BusinessException("Business error");
    assertEquals("Business error", ex1.getMessage());
    assertEquals(400, ex1.getStatusCode());

    BusinessException ex2 = new BusinessException("Custom error", 422);
    assertEquals("Custom error", ex2.getMessage());
    assertEquals(422, ex2.getStatusCode());
  }
}
