package com.github.warada_v2.services;

import com.github.warada_v2.db_entities.Chapter;
import com.github.warada_v2.models.PageRequest;
import com.github.warada_v2.repositories.ChapterRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;


@ApplicationScoped
public class ChapterServiceImpl implements ChapterService {
    @Inject
    ChapterRepository chapterRepository;

    public Chapter getChapterById(Long id) {
       return chapterRepository.findByIdOptional(id).orElse(null);
    }

    public List<Chapter> pageableChapterByComicId(Long comicId, PageRequest pageableRequest) {
        return chapterRepository.findByComicId(comicId, pageableRequest.toSort(), pageableRequest.toPage());
    }

    public Chapter getChapterBySlug(String slug) {
        return new Chapter();
    }

}
