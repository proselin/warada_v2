package com.github.warada_v2;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ChapterRepository implements PanacheRepository<Chapter> {

    public Optional<Chapter> findByIdOptional(Long aLong) {
        return Optional.ofNullable(findById(aLong));
    }

    public Optional<Chapter> findBySlug(String slug) {
        return find("slug = ?1",slug ).singleResultOptional();
    }

    public List<Chapter> findByComicId(Long comicId, Sort sort, Page page) {
        return find("comicId = ?1", comicId, sort, page).list();
    }
}
