package com.github.warada_v2.shared.exception_mappers;

import com.github.warada_v2.shared.models.ApiError;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
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
    // JAX-RS/RESTEasy Reactive report routing outcomes (404 for unmatched
    // routes, 405, etc.) as WebApplicationException with a client-error
    // status. These are expected client-facing conditions, not server
    // failures, so they must be passed through as-is instead of being
    // logged as ERROR and rewritten into a 500 response.
    if (exception instanceof WebApplicationException webApplicationException) {
      Response response = webApplicationException.getResponse();
      if (response.getStatus() < 500) {
        log.debugf(exception, "Client error response: %d", response.getStatus());
        return response;
      }
    }

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
