package com.github.warada_v2.core.resources;

import com.github.warada_v2.core.repositories.ChapterRepository;
import com.github.warada_v2.core.repositories.ComicRepository;
import com.github.warada_v2.shared.entities.Chapter;
import com.github.warada_v2.shared.exceptions.ResourceNotFoundException;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/internal")
public class InternalResource {

    @Inject ComicRepository comicRepository;
    @Inject ChapterRepository chapterRepository;

    @GET
    @Path("/comics/exists")
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    public Response comicExists(@QueryParam("slug") String slug) {
        boolean exists = comicRepository.existsByOriginPathParams(slug);
        if (exists) {
            return Response.ok().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/chapters/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    public Chapter getChapter(@PathParam("id") Long id) {
        return chapterRepository.findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", id));
    }
}
