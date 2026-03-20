package com.github.warada_v2.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.github.warada_v2.db_entities.Comic;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComicRepositoryImpl implements ComicRepository {

    public List<Comic> findPageableComics(Sort sort, Page page) {
        return findAll(sort).page(page).list();
    }

    public Boolean existedBySlugAndComicId(String slugAndComicId) {
        return count("originPathParams = ?1", slugAndComicId ) > 0;
    }

    public Optional<Comic> findBySlugAndComicId(String slugAndComicId) {
        return find("originPathParams", slugAndComicId).firstResultOptional();
    }

    public List<Comic> findComicsDueForUpdate(LocalDateTime now) {
        return list("updateOn != 'NONE' AND nextUpdateDate IS NOT NULL AND nextUpdateDate <= ?1", now);
    }
}
