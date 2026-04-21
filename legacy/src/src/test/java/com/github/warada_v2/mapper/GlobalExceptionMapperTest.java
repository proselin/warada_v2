package com.github.warada_v2.mapper;

import com.github.warada_v2.models.ApiError;
import org.junit.jupiter.api.Test;
import jakarta.ws.rs.core.Response;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
