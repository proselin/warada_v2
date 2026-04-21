package com.github.warada_v2.shared.models;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

public class ApiResponse<T> {
    public int status;
    public MultivaluedMap<String, Object> headers;
    public T body;

    public static <T> ApiResponse<T> from(Response res, Class<T> type) {
        ApiResponse<T> api = new ApiResponse<>();
        api.status = res.getStatus();
        api.headers = res.getHeaders();
        api.body = res.readEntity(type);
        res.close();
        return api;
    }
}
