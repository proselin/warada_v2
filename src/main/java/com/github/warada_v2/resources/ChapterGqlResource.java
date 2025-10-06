package com.github.warada_v2.resources;

import com.github.warada_v2.db_entities.Chapter;
import com.github.warada_v2.models.PageRequest;
import com.github.warada_v2.services.ChapterService;
import jakarta.inject.Inject;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import java.util.List;

@GraphQLApi
@JBossLog
public class ChapterGqlResource {
    @Inject
    ChapterService chapterService;

    @Query("detailChapterById")
    public Chapter getChapterById(Long id) {
        return chapterService.getChapterById(id);
    }

    @Query("detailChapterBySlug")
    public Chapter getChapterBySlug(String slug) {
        return chapterService.getChapterBySlug(slug);
    }

    @Query("chapters")
    public List<Chapter> pageableChapterByComicId(Long comicId, PageRequest pageRequest) {
        return chapterService.pageableChapterByComicId(comicId, pageRequest);
    }
}
