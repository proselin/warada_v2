package com.github.warada_v2.crawl.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RegisterRestClient(configKey = "core-service")
@Path("/internal")
public interface CoreServiceClient {

    @GET
    @Path("/comics/exists")
    @Produces(MediaType.APPLICATION_JSON)
    Response comicExists(@QueryParam("slug") String slug);
}
