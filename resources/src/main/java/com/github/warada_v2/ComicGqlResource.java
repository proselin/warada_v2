package com.github.warada_v2;

import com.github.warada_v2.models.PageRequest;
import io.smallrye.common.annotation.NonBlocking;
import jakarta.inject.Inject;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import java.util.List;

@JBossLog
@GraphQLApi
public class ComicGqlResource {

    @Inject
    ComicService comicService;

    @Query
    @NonBlocking
    public String ping() {
        log.debug("Loaded comic hello");
        return "Pong";
    }

    @Query("comics")
    @NonBlocking
    public List<Comic> comics(PageRequest pageRequest) {
        return comicService.getPageableComics(pageRequest);
    }

    @Query("comic")
    @NonBlocking
    public Comic comic(Long id) {
        return comicService.findById(id);
    }
}
