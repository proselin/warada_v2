package com.github.warada_v2.resources;

import com.github.warada_v2.models.CreateComicRequest;
import com.github.warada_v2.services.ComicService;

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
@Path("/api/v1/comics")
public class ComicRestResource {

    @Inject
    ComicService comicService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> createComic(CreateComicRequest request) {
        return Uni.createFrom().item(() -> {
            var comic = comicService.createComic(request);
            return Response.status(Response.Status.CREATED).entity(comic).build();
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> deleteComic(@PathParam("id") Long id) {
        return Uni.createFrom().item(() -> {
            comicService.deleteComic(id);
            return Response.noContent().build();
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
