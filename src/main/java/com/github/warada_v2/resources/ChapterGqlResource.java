package com.github.warada_v2.resources;

import com.github.warada_v2.models.ChapterDetailGql;
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
    public ChapterDetailGql getChapterById(Long id) {
        return ChapterDetailGql.fromEntity(chapterService.getChapterById(id));
    }

    @Query("detailChapterBySlug")
    public ChapterDetailGql getChapterBySlug(String slug) {
        return ChapterDetailGql.fromEntity(chapterService.getChapterBySlug(slug));
    }

    @Query("chapters")
    public List<ChapterDetailGql> pageableChapterByComicId(Long comicId, PageRequest pageRequest) {
        return ChapterDetailGql.fromEntities(chapterService.pageableChapterByComicId(comicId, pageRequest));
    }
}
