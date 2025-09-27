package com.github.warada_v2;

import io.smallrye.common.annotation.NonBlocking;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import java.util.List;

@JBossLog
@GraphQLApi
public class ComicGqlResource {

    @Query
    @NonBlocking
    public String hello() {
        log.debug("Loaded comic hello");
        return "Hello from Quarkus REST";
    }

    @Query("comics")
    @NonBlocking
    public List<Comic> comics() {
        var comic =   new Comic();
        comic.setId(1L);
        comic.setTitle("Comic 1");
        comic.setDescription("Comic 1");
        comic.setAuthor("Author 1");

        return List.of(
            comic
        );
    }
}
