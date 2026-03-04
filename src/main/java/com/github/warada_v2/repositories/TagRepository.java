package com.github.warada_v2.repositories;

import com.github.warada_v2.db_entities.Tag;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends PanacheRepository<Tag> {

    /**
     * Find a tag by its normalized (lowercase) name.
     */
    Optional<Tag> findByNormalizedName(String normalizedName);

    /**
     * Find all tags whose normalized name is in the given list.
     */
    List<Tag> findByNormalizedNameIn(List<String> normalizedNames);

    /**
     * Find all tags ordered by comic count descending.
     */
    List<Tag> findAllOrderByComicCountDesc();
}
