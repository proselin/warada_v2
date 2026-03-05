package com.github.warada_v2.repositories;

import com.github.warada_v2.db_entities.Tag;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TagRepositoryImpl implements TagRepository {

    @Override
    public Optional<Tag> findByNormalizedName(String normalizedName) {
        return find("normalizedName", normalizedName).firstResultOptional();
    }

    @Override
    public List<Tag> findByNormalizedNameIn(List<String> normalizedNames) {
        return list("normalizedName in ?1", normalizedNames);
    }

    @Override
    public List<Tag> findAllOrderByComicCountDesc() {
        return list("ORDER BY comicCount DESC");
    }
}
