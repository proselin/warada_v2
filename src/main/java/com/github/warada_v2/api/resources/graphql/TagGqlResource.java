package com.github.warada_v2.api.resources.graphql;

import com.github.warada_v2.shared.services.TagService;
import com.github.warada_v2.shared.entities.Tag;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
public class TagGqlResource {

  @Inject TagService tagService;

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
