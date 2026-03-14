package com.github.warada_v2.resources;

import java.util.List;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import com.github.warada_v2.db_entities.Comic;
import com.github.warada_v2.models.PageRequest;
import com.github.warada_v2.models.UpdateComicRequest;
import com.github.warada_v2.services.ComicService;

import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
@GraphQLApi
public class ComicGqlResource {

    @Inject
    ComicService comicService;

    @Query
    @Blocking
    public String ping() {
        log.debug("Loaded comic hello");
        return "Pong";
    }

    @Query("comics")
    @Blocking
    public List<Comic> comics(PageRequest pageRequest) {
        return comicService.getPageableComics(pageRequest);
    }

    @Query("comic")
    @Blocking
    public Comic comic(@Name("id") Long id) {
        return comicService.findById(id);
    }

    @Mutation("updateComic")
    @Blocking
    public Comic updateComic(@Name("id") Long id, @Name("input") UpdateComicRequest input) {
        return comicService.updateComic(id, input);
    }
}
