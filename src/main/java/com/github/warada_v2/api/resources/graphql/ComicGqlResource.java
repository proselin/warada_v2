package com.github.warada_v2.api.resources.graphql;

import com.github.warada_v2.shared.services.ComicService;
import com.github.warada_v2.shared.entities.Comic;
import com.github.warada_v2.shared.models.PageRequest;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.logging.Logger;

@GraphQLApi
public class ComicGqlResource {

  private static final Logger log = Logger.getLogger(ComicGqlResource.class);

  @Inject ComicService comicService;

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
}
