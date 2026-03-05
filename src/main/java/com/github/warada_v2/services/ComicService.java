package com.github.warada_v2.services;

import com.github.warada_v2.db_entities.Comic;
import com.github.warada_v2.models.PageRequest;

import java.util.List;

public interface ComicService {
    List<Comic> getPageableComics(PageRequest pageRequest);
    Comic findById(Long id);
}
