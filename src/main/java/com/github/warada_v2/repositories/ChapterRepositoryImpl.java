package com.github.warada_v2.repositories;

import com.github.warada_v2.db_entities.Chapter;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ChapterRepositoryImpl implements ChapterRepository {

    public Optional<Chapter> findByIdOptional(Long aLong) {
        return Optional.ofNullable(findById(aLong));
    }

    public Optional<Chapter> findBySlug(String slug) {
        return find("slug = ?1",slug ).singleResultOptional();
    }

    public List<Chapter> findByComicId(Long comicId, Sort sort, Page page) {
        return find("comic.id = ?1", sort, comicId).page(page).list();
    }

    public void persistOrUpdate(Chapter chapter) {
        if (chapter.getId() == null) {
            persist(chapter);
            return;
        }
        getEntityManager().merge(chapter);
    }
}
