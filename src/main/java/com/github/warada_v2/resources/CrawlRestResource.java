package com.github.warada_v2.resources;

import org.jboss.resteasy.reactive.RestStreamElementType;

import com.github.warada_v2.models.CrawlNettruyenChapterRequest;
import com.github.warada_v2.models.CrawlNettruyenComicRequest;
import com.github.warada_v2.services.CrawlService;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.OutboundSseEvent;
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
    public Uni<Response> crawlIndividualComic(CrawlNettruyenComicRequest request) {
        return Uni.createFrom().item(() -> {
            crawlService.crawlNettruyenComic(request.slugNId());
            return Response.ok("Success").build();
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @POST
    @Path("/nettruyen/chapter")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> crawlChapter(CrawlNettruyenChapterRequest request) {
        return Uni.createFrom().item(() -> {
            crawlService.crawlNettruyenChapter(request.chapterId());
            return Response.ok("Success").build();
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @GET
    @Path("/nettruyen/sse/comic")
    @RestStreamElementType("application/json")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<OutboundSseEvent> crawlChapterByComic(
            @QueryParam("slug-n-id") String slugNId
    ) {
        return crawlService.crawlNettruyenSSEChapter(slugNId);
    }
}
