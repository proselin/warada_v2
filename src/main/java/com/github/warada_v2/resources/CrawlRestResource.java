package com.github.warada_v2.resources;

import com.github.warada_v2.db_entities.Comic;
import com.github.warada_v2.models.CrawlNettruyenRequest;
import com.github.warada_v2.services.CrawlService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

@JBossLog
@Path("/api/v1/crawl")
public class CrawlRestResource {

    @Inject
    CrawlService crawlService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        log.debug("Loaded crawl hello");
        return "Hello from Quarkus REST";
    }

    @POST
    @Path("/comic/nettruyen")
    @Produces(MediaType.APPLICATION_JSON)
    public Comic crawlIndividualComic(
            @RequestBody CrawlNettruyenRequest request
    ) {
        return crawlService.crawlNettruyenComic(request.getUrl());
    }
}
