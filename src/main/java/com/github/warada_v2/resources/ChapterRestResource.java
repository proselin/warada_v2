package com.github.warada_v2.resources;

import com.github.warada_v2.models.ChapterDetailGql;
import com.github.warada_v2.models.CreateChapterRequest;
import com.github.warada_v2.services.ChapterService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
@Path("/api/v1/chapters")
public class ChapterRestResource {

    @Inject
    ChapterService chapterService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createChapter(CreateChapterRequest request) {
        var chapter = chapterService.createChapter(request);
        return Response.status(Response.Status.CREATED).entity(ChapterDetailGql.fromEntity(chapter)).build();
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteChapter(@PathParam("id") Long id) {
        chapterService.deleteChapter(id);
        return Response.noContent().build();
    }
}
