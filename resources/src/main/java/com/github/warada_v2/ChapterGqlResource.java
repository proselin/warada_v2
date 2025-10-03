package com.github.warada_v2;

import com.github.warada_v2.models.PageRequest;
import io.smallrye.common.annotation.NonBlocking;
import jakarta.inject.Inject;
import lombok.extern.jbosslog.JBossLog;
import lombok.extern.log4j.Log4j2;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import javax.print.attribute.standard.PagesPerMinute;
import java.util.List;

@GraphQLApi
@JBossLog
public class ChapterGqlResource {
    @Inject
    ChapterService chapterService;

    @Query("chapter-by-id")
    public Chapter getChapterById(Long id) {
        return chapterService.getChapterById(id);
    }

    @Query("chapter-by-slug")
    public Chapter getChapterBySlug(String slug) {
        return chapterService.getChapterBySlug(slug);
    }

    @Query("chapters")
    public List<Chapter> pageableChapterByComicId(Long comicId, PageRequest pageRequest) {
        return chapterService.pageableChapterByComicId(comicId, pageRequest);
    }
}
