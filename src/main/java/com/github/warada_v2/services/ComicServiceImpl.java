package com.github.warada_v2.services;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import com.github.warada_v2.commons.enums.CrawlStatus;
import com.github.warada_v2.db_entities.Comic;
import com.github.warada_v2.exceptions.ResourceNotFoundException;
import com.github.warada_v2.models.CreateComicRequest;
import com.github.warada_v2.models.PageRequest;
import com.github.warada_v2.models.UpdateComicRequest;
import com.github.warada_v2.repositories.ComicRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ComicServiceImpl implements ComicService {
    private static final Logger log = Logger.getLogger(ComicServiceImpl.class);
    @Inject
    ComicRepository comicRepository;

    @Inject
    TagService tagService;

    public List<Comic> getPageableComics(PageRequest pageRequest) {
        if (pageRequest == null) {
            pageRequest = new PageRequest();
        }
        return comicRepository.findPageableComics(pageRequest.toSort(), pageRequest.toPage());
    }

    public Comic findById(Long id) {
        return comicRepository.findByIdOptional(id).orElseThrow(NotFoundException::new);
    }

    @Transactional
    public Comic createComic(CreateComicRequest request) {
        Comic comic = Comic.builder()
                .title(request.title())
                .description(request.description())
                .author(request.author())
                .status(request.status() != null ? request.status() : "ongoing")
                .slug(request.slug())
                .originId(request.originId())
                .originUrl(request.originUrl())
                .originPathParams(request.originPathParams())
                .crawlingStatus(CrawlStatus.INIT)
                .chapterCount(0)
                .tags(new ArrayList<>())
                .build();
        if (request.tags() != null && !request.tags().isEmpty()) {
            comic.setTags(tagService.resolveOrCreateTags(request.tags()));
        }
        comicRepository.persist(comic);
        log.infof("Created comic id=%d slug=%s", comic.getId(), comic.getSlug());
        return comic;
    }

    @Transactional
    public Comic updateComic(Long id, UpdateComicRequest request) {
        Comic comic = comicRepository.findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comic", "id", id));
        if (request.title() != null) comic.setTitle(request.title());
        if (request.description() != null) comic.setDescription(request.description());
        if (request.author() != null) comic.setAuthor(request.author());
        if (request.status() != null) comic.setStatus(request.status());
        comicRepository.persist(comic);
        log.infof("Updated comic id=%d", id);
        return comic;
    }

    @Transactional
    public void deleteComic(Long id) {
        boolean deleted = comicRepository.deleteById(id);
        if (!deleted) {
            throw new ResourceNotFoundException("Comic", "id", id);
        }
        log.infof("Deleted comic id=%d", id);
    }

}
