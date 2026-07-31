package com.github.warada_v2.api.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RegisterRestClient(configKey = "nettruyen")
public interface NettruyenRestService {

    @GET
    @Path("/truyen-tranh/{slug-n-id}")
    @Produces(MediaType.TEXT_HTML)
    Response getContent(@PathParam("slug-n-id") String slugNId);

    @GET
    @Path("/Comic/Services/ComicService.asmx/ChapterList")
    @Produces(MediaType.APPLICATION_JSON)
    Response getComicChapterList(@QueryParam("slug") String slug,
                                 @QueryParam("comicId") Long comicId);
}
