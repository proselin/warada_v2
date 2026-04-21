package com.github.warada_v2.resources;

import java.util.List;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import com.github.warada_v2.models.ChapterDetailGql;
import com.github.warada_v2.models.PageRequest;
import com.github.warada_v2.models.UpdateChapterRequest;
import com.github.warada_v2.services.ChapterService;

import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;

@GraphQLApi
public class ChapterGqlResource {
    @Inject
    ChapterService chapterService;

    @Query("detailChapterById")
    @Blocking
    public ChapterDetailGql getChapterById(Long id) {
        return ChapterDetailGql.fromEntity(chapterService.getChapterById(id));
    }

    @Query("detailChapterBySlug")
    @Blocking
    public ChapterDetailGql getChapterBySlug(String slug) {
        return ChapterDetailGql.fromEntity(chapterService.getChapterBySlug(slug));
    }

    @Query("chapters")
    @Blocking
    public List<ChapterDetailGql> pageableChapterByComicId(Long comicId, PageRequest pageRequest) {
        return ChapterDetailGql.fromEntities(chapterService.pageableChapterByComicId(comicId, pageRequest));
    }

    @Mutation("updateChapter")
    @Blocking
    public ChapterDetailGql updateChapter(@Name("id") Long id, @Name("input") UpdateChapterRequest input) {
        return ChapterDetailGql.fromEntity(chapterService.updateChapter(id, input));
    }
}
