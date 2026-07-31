package com.github.warada_v2.crawl.resources.rest;

import com.github.warada_v2.crawl.models.CrawlNettruyenComicRequest;
import com.github.warada_v2.crawl.services.CrawlProgressBroadcaster;
import com.github.warada_v2.crawl.services.CrawlService;
import com.github.warada_v2.crawl.models.CrawlProgressEvent;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestStreamElementType;

@Path("/api/v1/crawl")
public class CrawlRestResource {

  @Inject CrawlService crawlService;

  @Inject CrawlProgressBroadcaster crawlProgressBroadcaster;

  @POST
  @Path("/nettruyen/comic")
  @Produces(MediaType.TEXT_PLAIN)
  @Consumes(MediaType.APPLICATION_JSON)
  @Blocking
  public Response crawlIndividualComic(CrawlNettruyenComicRequest request) throws Exception {
    crawlService.crawlNettruyenComic(request.slugNId());
    return Response.ok("Success").build();
  }

  @POST
  @Path("/nettruyen/comic/{slug}/retry")
  @Produces(MediaType.TEXT_PLAIN)
  @Blocking
  public Response retryFailedChapters(@PathParam("slug") String comicSlug) {
    crawlService.retryFailedChapters(comicSlug);
    return Response.ok("Retry queued").build();
  }

  @GET
  @Path("/progress/{comicSlug}")
  @RestStreamElementType("application/json")
  @Produces(MediaType.SERVER_SENT_EVENTS)
  public Multi<CrawlProgressEvent> subscribeToCrawlProgress(
      @PathParam("comicSlug") String comicSlug) {
    return crawlProgressBroadcaster.subscribe(comicSlug);
  }
}
