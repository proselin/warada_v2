package com.github.warada_v2.services;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.github.warada_v2.commons.enums.CrawlStatus;
import com.github.warada_v2.db_entities.Chapter;
import com.github.warada_v2.models.ChapterCrawlMessage;
import com.github.warada_v2.repositories.ChapterRepository;

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
    void testConsumeRethrowsWhenChapterCrawlFails() {
        Chapter chapter = new Chapter();
        chapter.setId(42L);
        chapter.setCrawlingStatus(CrawlStatus.INIT);

        ChapterCrawlMessage message = new ChapterCrawlMessage(42L, "one-piece-12345");

        when(chapterRepository.findByIdOptional(42L)).thenReturn(Optional.of(chapter));
        when(chapterRepository.countByComicSlugAndStatusIn("one-piece-12345", CrawlStatus.INIT, CrawlStatus.STARTED))
                .thenReturn(1L);

        RuntimeException failure = new RuntimeException("boom");
        org.mockito.Mockito.doThrow(failure).when(crawlService).crawlChapter(chapter);

        assertThrows(RuntimeException.class, () -> consumer.consume(message));
        verify(broadcaster, atLeastOnce()).emit(any(), any());
    }
}
