package com.github.warada_v2.api.services;

import com.github.warada_v2.shared.entities.Chapter;
import com.github.warada_v2.shared.models.PageRequest;
import com.github.warada_v2.api.repositories.ChapterRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import java.util.List;

@ApplicationScoped
public class ChapterServiceImpl implements ChapterService {

    private static final Logger log = Logger.getLogger(ChapterServiceImpl.class);

    @Inject
    ChapterRepository chapterRepository;

    @Override
    public Chapter getChapterById(Long id) {
        return chapterRepository.findByIdOptional(id).orElse(null);
    }

    @Override
    public List<Chapter> pageableChapterByComicId(Long comicId, PageRequest pageableRequest) {
        return chapterRepository.findByComicId(comicId, pageableRequest.toSort(), pageableRequest.toPage());
    }

    @Override
    public Chapter getChapterBySlug(String slug) {
        return chapterRepository.findBySlug(slug).orElse(null);
    }
}
