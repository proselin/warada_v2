package com.github.warada_v2.resources;

import com.github.warada_v2.models.CrawlNettruyenChapterRequest;
import com.github.warada_v2.models.CrawlNettruyenComicRequest;
import com.github.warada_v2.models.CrawlNettruyenSSEComic;
import com.github.warada_v2.services.CrawlService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
@Path("/api/v1/crawl")
public class CrawlRestResource {

    @Inject
    CrawlService crawlService;

    @POST
    @Path("/nettruyen/comic")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response crawlIndividualComic(CrawlNettruyenComicRequest request) throws Exception {
        crawlService.crawlNettruyenComic(request.slugNId());
        return Response.ok("Success").build();
    }

    @POST
    @Path("/nettruyen/chapter")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response crawlChapter(CrawlNettruyenChapterRequest request) throws Exception {
        crawlService.crawlNettruyenChapter(request.chapterId());
        return Response.ok("Success").build();
    }


    @GET
    @Path("/nettruyen/sse/comic")
    @Blocking
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.SERVER_SENT_EVENTS)
    public Multi<CrawlNettruyenSSEComic> crawlChapterByComic(
            @QueryParam("slug-n-id") String slugNId
    ) throws Exception {
        return crawlService.crawlNettruyenSSEChapter(slugNId);
    }
}
