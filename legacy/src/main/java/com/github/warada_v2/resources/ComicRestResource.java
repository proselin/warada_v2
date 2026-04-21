package com.github.warada_v2.resources;

import com.github.warada_v2.models.CreateComicRequest;
import com.github.warada_v2.services.ComicService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/comics")
public class ComicRestResource {

    @Inject
    ComicService comicService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createComic(CreateComicRequest request) {
        var comic = comicService.createComic(request);
        return Response.status(Response.Status.CREATED).entity(comic).build();
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteComic(@PathParam("id") Long id) {
        comicService.deleteComic(id);
        return Response.noContent().build();
    }
}
