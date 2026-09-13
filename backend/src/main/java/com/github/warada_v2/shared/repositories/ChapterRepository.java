package com.github.warada_v2.shared.repositories;

import com.github.warada_v2.shared.entities.Chapter;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ChapterRepository implements PanacheRepository<Chapter> {

  public Optional<Chapter> findByIdWithComic(Long id) {
    return find("SELECT c FROM Chapter c LEFT JOIN FETCH c.comic WHERE c.id = ?1", id)
        .firstResultOptional();
  }

  public Optional<Chapter> findByOriginPathParams(String originPathParams) {
    return find("originPathParams", originPathParams).firstResultOptional();
  }

  public Optional<Chapter> findBySlug(String slug) {
    return find("originPathParams", slug).firstResultOptional();
  }

  public List<Chapter> findByComicId(Long comicId, Sort sort, Page page) {
    return find("comic.id = ?1", sort, comicId).page(page).list();
  }

  public void persistOrUpdate(Chapter chapter) {
    if (chapter.getId() == null) {
      getEntityManager().persist(chapter);
    } else {
      getEntityManager().merge(chapter);
    }
  }
}
