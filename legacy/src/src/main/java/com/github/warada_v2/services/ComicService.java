package com.github.warada_v2.services;

import java.util.List;

import com.github.warada_v2.db_entities.Comic;
import com.github.warada_v2.models.CreateComicRequest;
import com.github.warada_v2.models.PageRequest;
import com.github.warada_v2.models.UpdateComicRequest;

public interface ComicService {
    List<Comic> getPageableComics(PageRequest pageRequest);
    Comic findById(Long id);
    Comic createComic(CreateComicRequest request);
    Comic updateComic(Long id, UpdateComicRequest request);
    void deleteComic(Long id);
}
