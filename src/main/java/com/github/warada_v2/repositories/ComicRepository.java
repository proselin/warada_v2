package com.github.warada_v2.repositories;

import com.github.warada_v2.db_entities.Comic;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ComicRepository implements PanacheRepository<Comic> {

    public List<Comic> findPageableComics(Sort sort, Page page) {
        return findAll(sort).page(page).list();
    }

    public Boolean existedByOriginUrl(String originUrl) {
        return count("originUrl = ?1", originUrl) > 0;
    }
}
