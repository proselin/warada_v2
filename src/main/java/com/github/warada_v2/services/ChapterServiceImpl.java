package com.github.warada_v2.services;

import java.util.List;

import org.jboss.logging.Logger;

import com.github.warada_v2.commons.enums.CrawlStatus;
import com.github.warada_v2.db_entities.Chapter;
import com.github.warada_v2.exceptions.ResourceNotFoundException;
import com.github.warada_v2.models.CreateChapterRequest;
import com.github.warada_v2.models.PageRequest;
import com.github.warada_v2.models.UpdateChapterRequest;
import com.github.warada_v2.repositories.ChapterRepository;
import com.github.warada_v2.repositories.ComicRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;


@ApplicationScoped
public class ChapterServiceImpl implements ChapterService {
    private static final Logger log = Logger.getLogger(ChapterServiceImpl.class);
    @Inject
    ChapterRepository chapterRepository;

    @Inject
    ComicRepository comicRepository;

    public Chapter getChapterById(Long id) {
        return chapterRepository.findByIdOptional(id).orElse(null);
    }

    public List<Chapter> pageableChapterByComicId(Long comicId, PageRequest pageableRequest) {
        return chapterRepository.findByComicId(comicId, pageableRequest.toSort(), pageableRequest.toPage());
    }

    public Chapter getChapterBySlug(String slug) {
        return chapterRepository.findBySlug(slug).orElse(null);
    }

    @Transactional
    public Chapter createChapter(CreateChapterRequest request) {
        var comic = comicRepository.findByIdOptional(request.comicId())
                .orElseThrow(() -> new ResourceNotFoundException("Comic", "id", request.comicId()));
        Chapter chapter = Chapter.builder()
                .chapterNumber(request.chapterNumber())
                .type(request.type())
                .position(request.position())
                .originUrl(request.originUrl())
                .originPathParams(request.originPathParams())
                .crawlingStatus(CrawlStatus.INIT)
                .comic(comic)
                .build();
        chapterRepository.persist(chapter);
        log.infof("Created chapter id=%d chapterNumber=%s comicId=%d", chapter.getId(), chapter.getChapterNumber(), request.comicId());
        return chapter;
    }

    @Transactional
    public Chapter updateChapter(Long id, UpdateChapterRequest request) {
        Chapter chapter = chapterRepository.findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", id));
        if (request.chapterNumber() != null) chapter.setChapterNumber(request.chapterNumber());
        if (request.type() != null) chapter.setType(request.type());
        if (request.position() != null) chapter.setPosition(request.position());
        chapterRepository.persist(chapter);
        log.infof("Updated chapter id=%d", id);
        return chapter;
    }

    @Transactional
    public void deleteChapter(Long id) {
        boolean deleted = chapterRepository.deleteById(id);
        if (!deleted) {
            throw new ResourceNotFoundException("Chapter", "id", id);
        }
        log.infof("Deleted chapter id=%d", id);
    }

}
