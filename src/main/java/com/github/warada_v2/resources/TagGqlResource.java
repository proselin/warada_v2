package com.github.warada_v2.resources;

import java.util.List;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import com.github.warada_v2.db_entities.Tag;
import com.github.warada_v2.services.TagService;

import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;

@GraphQLApi
public class TagGqlResource {

    @Inject
    TagService tagService;

    @Query("tags")
    @Blocking
    public List<Tag> tags() {
        return tagService.getAllTags();
    }

    @Query("tag")
    @Blocking
    public Tag tag(@Name("id") Long id) {
        return tagService.findById(id);
    }
}
