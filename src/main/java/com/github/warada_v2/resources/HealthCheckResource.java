package com.github.warada_v2.resources;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/zhealth")
public class HealthCheckResource {
    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> ping() {
        return Uni.createFrom().item("pong");
    }
}
