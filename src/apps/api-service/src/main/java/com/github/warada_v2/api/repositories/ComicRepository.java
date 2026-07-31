package com.github.warada_v2.api.repositories;

import com.github.warada_v2.shared.entities.Comic;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ComicRepository implements PanacheRepository<Comic> {

    public boolean existsByOriginPathParams(String originPathParams) {
        return count("originPathParams = ?1", originPathParams) > 0;
    }

    public Optional<Comic> findByOriginPathParams(String originPathParams) {
        return find("originPathParams", originPathParams).firstResultOptional();
    }

    public Optional<Comic> findBySlug(String slug) {
        return find("slug", slug).firstResultOptional();
    }

    public List<Comic> findPageableComics(Sort sort, Page page) {
        return findAll(sort).page(page).list();
    }
}
