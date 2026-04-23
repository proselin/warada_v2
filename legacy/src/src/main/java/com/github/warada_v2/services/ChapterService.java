package com.github.warada_v2.services;

import java.util.List;

import com.github.warada_v2.db_entities.Chapter;
import com.github.warada_v2.models.CreateChapterRequest;
import com.github.warada_v2.models.PageRequest;
import com.github.warada_v2.models.UpdateChapterRequest;

public interface ChapterService {
    Chapter getChapterById(Long id);
    List<Chapter> pageableChapterByComicId(Long comicId, PageRequest pageableRequest);
    Chapter getChapterBySlug(String slug);
    Chapter createChapter(CreateChapterRequest request);
    Chapter updateChapter(Long id, UpdateChapterRequest request);
    void deleteChapter(Long id);
}
