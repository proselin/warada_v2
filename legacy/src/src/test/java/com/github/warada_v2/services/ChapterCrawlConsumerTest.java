package com.github.warada_v2.services;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.github.warada_v2.commons.enums.CrawlStatus;
import com.github.warada_v2.db_entities.Chapter;
import com.github.warada_v2.models.ChapterCrawlMessage;
import com.github.warada_v2.models.CrawlProgressEvent;
import com.github.warada_v2.repositories.ChapterRepository;

import io.vertx.core.json.JsonObject;

class ChapterCrawlConsumerTest {

    @Mock
    ChapterRepository chapterRepository;

    @Mock
    CrawlService crawlService;

    @Mock
    CrawlProgressBroadcaster broadcaster;

    @InjectMocks
    ChapterCrawlConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testConsumeThrowsWhenChapterMissing() {
        ChapterCrawlMessage message = new ChapterCrawlMessage(123L, "test-slug");
        when(chapterRepository.findByIdOptional(123L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> consumer.consume(JsonObject.mapFrom(message)));

        verify(broadcaster, atLeastOnce()).emit(eq("test-slug"), any(CrawlProgressEvent.class));
    }

    @Test
    void testConsumeRethrowsCrawlFailures() {
        ChapterCrawlMessage message = new ChapterCrawlMessage(123L, "test-slug");
        Chapter chapter = new Chapter();
        chapter.setId(123L);
        chapter.setCrawlingStatus(CrawlStatus.INIT);

        when(chapterRepository.findByIdOptional(123L)).thenReturn(Optional.of(chapter));
        doThrow(new RuntimeException("boom")).when(crawlService).crawlChapter(chapter);

        assertThrows(RuntimeException.class, () -> consumer.consume(JsonObject.mapFrom(message)));

        verify(broadcaster, atLeastOnce()).emit(eq("test-slug"), any(CrawlProgressEvent.class));
    }
}

