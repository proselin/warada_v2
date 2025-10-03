package com.github.warada_v2;

import com.github.warada_v2.models.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@ApplicationScoped
public class ChapterService {
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
