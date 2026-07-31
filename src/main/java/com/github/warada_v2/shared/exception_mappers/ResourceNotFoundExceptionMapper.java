package com.github.warada_v2.shared.exception_mappers;

import com.github.warada_v2.shared.exceptions.ResourceNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ResourceNotFoundExceptionMapper implements ExceptionMapper<ResourceNotFoundException> {

  @Override
  public Response toResponse(ResourceNotFoundException exception) {
    return Response.status(Response.Status.NOT_FOUND).entity(exception.getMessage()).build();
  }
}
