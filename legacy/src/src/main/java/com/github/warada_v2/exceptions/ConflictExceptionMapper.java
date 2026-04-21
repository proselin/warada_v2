package com.github.warada_v2.exceptions;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class ConflictExceptionMapper implements ExceptionMapper<ConflictException> {
    @Override
    public Response toResponse(ConflictException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(exception.getMessage())
                .build();
    }
}
