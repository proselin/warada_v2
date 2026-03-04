package com.github.warada_v2.mapper;

import com.github.warada_v2.models.ApiError;
import jakarta.ws.rs.ext.*;
import jakarta.ws.rs.core.*;
import jakarta.inject.Singleton;
import java.util.UUID;

@Provider
@Singleton
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {

        String processId = UUID.randomUUID().toString();

        ApiError error = new ApiError(
                processId,
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Internal server error"
        );
        exception.printStackTrace();

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}