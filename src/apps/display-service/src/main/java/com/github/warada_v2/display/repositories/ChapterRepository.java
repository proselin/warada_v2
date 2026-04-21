package com.github.warada_v2.display.repositories;

import com.github.warada_v2.shared.entities.Chapter;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ChapterRepository implements PanacheRepository<Chapter> {

    public Optional<Chapter> findBySlug(String slug) {
        return find("originPathParams", slug).firstResultOptional();
    }

    public List<Chapter> findByComicId(Long comicId, Sort sort, Page page) {
        return find("comic.id = ?1", sort, comicId).page(page).list();
    }
}
