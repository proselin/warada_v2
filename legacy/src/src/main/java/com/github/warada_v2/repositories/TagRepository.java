package com.github.warada_v2.repositories;

import java.util.List;
import java.util.Optional;

import com.github.warada_v2.db_entities.Tag;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TagRepository implements PanacheRepository<Tag> {

    public Optional<Tag> findByNormalizedName(String normalizedName) {
        return find("normalizedName", normalizedName).firstResultOptional();
    }

    public List<Tag> findByNormalizedNameIn(List<String> normalizedNames) {
        return list("normalizedName in ?1", normalizedNames);
    }

    public List<Tag> findAllOrderByComicCountDesc() {
        return list("ORDER BY comicCount DESC");
    }
}
