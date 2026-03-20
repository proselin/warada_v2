package com.github.warada_v2.resources;

import com.github.warada_v2.models.ChapterDetailGql;
import com.github.warada_v2.models.CreateChapterRequest;
import com.github.warada_v2.services.ChapterService;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
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
    public Uni<Response> createChapter(CreateChapterRequest request) {
        return Uni.createFrom().item(() -> {
            var chapter = chapterService.createChapter(request);
            return Response.status(Response.Status.CREATED).entity(ChapterDetailGql.fromEntity(chapter)).build();
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> deleteChapter(@PathParam("id") Long id) {
        return Uni.createFrom().item(() -> {
            chapterService.deleteChapter(id);
            return Response.noContent().build();
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
