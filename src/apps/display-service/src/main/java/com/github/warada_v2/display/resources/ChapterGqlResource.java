package com.github.warada_v2.display.resources;

import com.github.warada_v2.display.models.ChapterDetailGql;
import com.github.warada_v2.shared.models.PageRequest;
import com.github.warada_v2.display.services.ChapterService;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import java.util.List;

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
}
