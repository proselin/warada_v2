package com.github.warada_v2;

import io.smallrye.common.annotation.NonBlocking;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
public class ImageGqlResource {

    @Query
    @NonBlocking
    public String hello() {
        return "Hello from Quarkus REST";
    }
}
