package com.github.warada_v2;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
@Path("/api/v1/crawl")
public class CrawlRestResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        log.debug("Loaded crawl hello");
        return "Hello from Quarkus REST";
    }
}
