package com.github.warada_v2.repositories;

import com.github.warada_v2.db_entities.Comic;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

import java.util.List;

public interface ComicRepository extends PanacheRepository<Comic> {
    List<Comic> findPageableComics(Sort sort, Page page);
    Boolean existedBySlugAndComicId(String slugAndComicId);
}
