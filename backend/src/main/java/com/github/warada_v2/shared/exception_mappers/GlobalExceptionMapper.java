package com.github.warada_v2.shared.exception_mappers;

import com.github.warada_v2.shared.models.ApiError;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;
import org.jboss.logging.Logger;

@Provider
@Singleton
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

  private static final Logger log = Logger.getLogger(GlobalExceptionMapper.class);

  @Override
  public Response toResponse(Throwable exception) {
    String processId = UUID.randomUUID().toString();
    log.errorf(exception, "Unhandled exception [processId=%s]", processId);

    ApiError error =
        new ApiError(
            processId,
            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            "Internal server error");
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(error)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }
}
