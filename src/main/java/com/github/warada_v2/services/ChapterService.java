package com.github.warada_v2.services;

import com.github.warada_v2.db_entities.Chapter;
import com.github.warada_v2.models.PageRequest;

import java.util.List;

public interface ChapterService {
    Chapter getChapterById(Long id);
    List<Chapter> pageableChapterByComicId(Long comicId, PageRequest pageableRequest);
    Chapter getChapterBySlug(String slug);
}
