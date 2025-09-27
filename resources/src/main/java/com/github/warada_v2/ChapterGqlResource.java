package com.github.warada_v2;

import io.smallrye.common.annotation.NonBlocking;
import lombok.extern.jbosslog.JBossLog;
import lombok.extern.log4j.Log4j2;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
@JBossLog
public class ChapterGqlResource {
    @Query
    @NonBlocking
    public String hello() {
        log.debug("Loaded Chapter hello");
        return "Hello from Quarkus REST";
    }
}
