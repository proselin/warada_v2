package com.github.warada_v2.repositories;

import com.github.warada_v2.db_entities.Comic;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ComicRepositoryImpl implements ComicRepository {

    public List<Comic> findPageableComics(Sort sort, Page page) {
        return findAll(sort).page(page).list();
    }

    public Boolean existedBySlugAndComicId(String slugAndComicId) {
        return count("originUrl LIKE ~ ?1", slugAndComicId + "$") > 0;
    }
}
