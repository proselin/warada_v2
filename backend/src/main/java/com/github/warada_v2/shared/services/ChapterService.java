package com.github.warada_v2.shared.services;

import com.github.warada_v2.shared.entities.Chapter;
import com.github.warada_v2.shared.models.PageRequest;
import java.util.List;

public interface ChapterService {
  Chapter getChapterById(Long id);

  List<Chapter> pageableChapterByComicId(Long comicId, PageRequest pageableRequest);

  Chapter getChapterBySlug(String slug);
}
