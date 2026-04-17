package com.github.warada_v2.repositories;

import com.github.warada_v2.commons.enums.CrawlStatus;
import com.github.warada_v2.db_entities.Chapter;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class ChapterRepositoryImpl implements ChapterRepository {

    public Optional<Chapter> findByIdOptional(Long aLong) {
        return Optional.ofNullable(findById(aLong));
    }

    public Optional<Chapter> findBySlug(String slug) {
        return find("slug = ?1",slug ).singleResultOptional();
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

    @Override
    public long countByComicSlugAndStatusIn(String comicSlug, CrawlStatus... statuses) {
        List<CrawlStatus> statusList = Arrays.asList(statuses);
        return count("comic.originPathParams = ?1 and crawlingStatus in ?2", comicSlug, statusList);
    }

    @Override
    public List<Chapter> findByComicSlugAndStatusIn(String comicSlug, CrawlStatus... statuses) {
        List<CrawlStatus> statusList = Arrays.asList(statuses);
        return list("comic.originPathParams = ?1 and crawlingStatus in ?2", comicSlug, statusList);
    }
}
