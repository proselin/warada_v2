package com.github.warada_v2.services;



import com.github.warada_v2.crawl.services.CrawlProgressBroadcaster;
import com.github.warada_v2.crawl.services.CrawlService;
import com.github.warada_v2.shared.repositories.ChapterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ChapterCrawlConsumerTest {

  @Mock ChapterRepository chapterRepository;

  @Mock CrawlService crawlService;

  @Mock CrawlProgressBroadcaster broadcaster;

  //    @InjectMocks
  //    ChapterCrawlConsumer consumer;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  //    @Test
  //    void testConsumeThrowsWhenChapterMissing() {
  //        ChapterCrawlMessage message = new ChapterCrawlMessage(123L, "test-slug");
  //        when(chapterRepository.findByIdOptional(123L)).thenReturn(Optional.empty());
  //
  //        assertThrows(IllegalStateException.class, () ->
  // consumer.consume(JsonObject.mapFrom(message)));
  //
  //        verify(broadcaster, atLeastOnce()).emit(eq("test-slug"), any(CrawlProgressEvent.class));
  //    }

  //    @Test
  //    void testConsumeRethrowsCrawlFailures() {
  //        ChapterCrawlMessage message = new ChapterCrawlMessage(123L, "test-slug");
  //        Chapter chapter = new Chapter();
  //        chapter.setId(123L);
  //        chapter.setCrawlingStatus(CrawlStatus.INIT);
  //
  //        when(chapterRepository.findByIdOptional(123L)).thenReturn(Optional.of(chapter));
  //        doThrow(new RuntimeException("boom")).when(crawlService).crawlChapter(chapter);
  //
  //        assertThrows(RuntimeException.class, () ->
  // consumer.consume(JsonObject.mapFrom(message)));
  //
  //        verify(broadcaster, atLeastOnce()).emit(eq("test-slug"), any(CrawlProgressEvent.class));
  //    }
}
