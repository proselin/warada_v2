package com.github.warada_v2.rest.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "nettruyen")
public interface NettruyenRestService {

    @GET
    @Path("/truyen-tranh/{slug-n-id}")
    @Produces(MediaType.TEXT_HTML)
    Response getComicHtmlContent(@PathParam("slug-n-id") String slugNId);

    @GET
    @Path("/Comic/Services/ComicService.asmx/ChapterList")
    @Produces(MediaType.APPLICATION_JSON)
    Response getComicChapterList(@QueryParam("slug") String slug,
                                 @QueryParam("comicId") Long comicId);
}
