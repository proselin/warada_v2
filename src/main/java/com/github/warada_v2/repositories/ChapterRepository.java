package com.github.warada_v2.repositories;

import com.github.warada_v2.db_entities.Chapter;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends PanacheRepository<Chapter> {
    Optional<Chapter> findBySlug(String slug);
    Optional<Chapter> findByIdOptional(Long aLong);
    List<Chapter> findByComicId(Long comicId, Sort sort, Page page);
}
