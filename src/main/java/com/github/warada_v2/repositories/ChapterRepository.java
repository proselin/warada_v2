package com.github.warada_v2.repositories;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.github.warada_v2.commons.enums.CrawlStatus;
import com.github.warada_v2.db_entities.Chapter;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ChapterRepository implements PanacheRepository<Chapter> {

    public Optional<Chapter> findBySlug(String slug) {
        return find("originPathParams", slug).firstResultOptional();
    }

    public List<Chapter> findByComicId(Long comicId, Sort sort, Page page) {
        return find("comic.id = ?1", sort, comicId).page(page).list();
    }

    public void persistOrUpdate(Chapter chapter) {
        if (chapter.getId() == null) {
            getEntityManager().persist(chapter);
            return;
        }
        getEntityManager().merge(chapter);
    }

    public long countByComicSlugAndStatusIn(String comicSlug, CrawlStatus... statuses) {
        List<CrawlStatus> statusList = Arrays.asList(statuses);
        return count("comic.originPathParams = ?1 and crawlingStatus in ?2", comicSlug, statusList);
    }

    public List<Chapter> findByComicSlugAndStatusIn(String comicSlug, CrawlStatus... statuses) {
        List<CrawlStatus> statusList = Arrays.asList(statuses);
        return list("comic.originPathParams = ?1 and crawlingStatus in ?2", comicSlug, statusList);
    }
}
