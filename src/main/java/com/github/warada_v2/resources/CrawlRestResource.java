package com.github.warada_v2.resources;

import com.github.warada_v2.models.CrawlNettruyenRequest;
import com.github.warada_v2.services.CrawlService;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

@JBossLog
@Path("/api/v1/crawl")
public class CrawlRestResource {

    @Inject
    CrawlService crawlService;

    @POST
    @Path("/comic/nettruyen")
    @Produces(MediaType.TEXT_PLAIN)
    public Response crawlIndividualComic(@RequestBody CrawlNettruyenRequest request) throws Exception {
        crawlService.crawlNettruyenComic(request.getSlugNId());
        return Response.ok("Success").build();
    }
}
