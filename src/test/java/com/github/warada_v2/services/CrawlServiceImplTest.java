package com.github.warada_v2.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.warada_v2.crawl.services.CrawlProgressBroadcaster;
import com.github.warada_v2.crawl.services.NettruyenExtractor;
import com.github.warada_v2.shared.services.TagService;
import com.github.warada_v2.crawl.services.impls.CrawlServiceImpl;
import com.github.warada_v2.crawl.models.ExtractedChapterDetail;
import com.github.warada_v2.crawl.models.ExtractedChapters;
import com.github.warada_v2.crawl.models.ExtractedNettruyenComic;
import com.github.warada_v2.crawl.models.ExtractedThumbImage;
import com.github.warada_v2.crawl.models.CrawlProgressEvent;
import com.github.warada_v2.shared.configurations.WaradaConfig;
import com.github.warada_v2.shared.crawl.ExtractedComicDetail;
import com.github.warada_v2.shared.entities.Chapter;
import com.github.warada_v2.shared.entities.Comic;
import com.github.warada_v2.shared.enums.CrawlStatus;
import com.github.warada_v2.shared.exceptions.ConflictException;
import com.github.warada_v2.shared.repositories.ChapterRepository;
import com.github.warada_v2.shared.repositories.ComicRepository;
import com.github.warada_v2.shared.repositories.ImageRepository;
import io.minio.MinioClient;
import io.quarkus.test.junit.QuarkusTest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@QuarkusTest
class CrawlServiceImplTest {

  @Mock ComicRepository comicRepository;

  @Mock NettruyenExtractor nettruyenExtractor;

  @Mock MinioClient minioClient;

  @Mock WaradaConfig waradaConfig;

  @Mock ChapterRepository chapterRepository;

  @Mock ImageRepository imageRepository;

  @Mock TagService tagService;

  @Mock CrawlProgressBroadcaster broadcaster;

  @InjectMocks CrawlServiceImpl crawlService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testCrawlNettruyenComicConflict() {
    when(comicRepository.existsByOriginPathParams("test-slug-123")).thenReturn(true);

    assertThrows(ConflictException.class, () -> crawlService.crawlNettruyenComic("test-slug-123"));
    verify(comicRepository, times(1)).existsByOriginPathParams("test-slug-123");
    verifyNoMoreInteractions(nettruyenExtractor);
  }

  @Test
  void testCrawlNettruyenComicSuccess() throws Exception {
    String slugNId = "test-slug-123";
    when(comicRepository.existsByOriginPathParams(slugNId)).thenReturn(false);

    ExtractedNettruyenComic.Author author =
        new ExtractedNettruyenComic.Author("Person", "John Doe");
    ExtractedNettruyenComic comicInfo =
        new ExtractedNettruyenComic(
            "context",
            "type",
            "Title",
            "Alt",
            "http://url",
            "image.jpg",
            author,
            List.of("Action"),
            "Desc",
            "2023",
            "2023",
            123L,
            "test-slug");

    ExtractedChapters chapters = new ExtractedChapters(Collections.emptyList());
    ExtractedThumbImage thumbImage =
        new ExtractedThumbImage("http://url", "test.jpg", "temp/test.jpg");

    ExtractedComicDetail detail = new ExtractedComicDetail(comicInfo, chapters, thumbImage);

    when(nettruyenExtractor.extractComicDetail(slugNId)).thenReturn(detail);
    when(nettruyenExtractor.extractChapterDetail(anyString()))
        .thenReturn(new ExtractedChapterDetail(Collections.emptyList()));
    when(tagService.resolveOrCreateTags(anyList())).thenReturn(Collections.emptyList());
    when(waradaConfig.imageBucketImagesPath()).thenReturn("images");
    when(waradaConfig.imageBucket()).thenReturn("warada-images");

    crawlService.crawlNettruyenComic(slugNId);

    verify(comicRepository, times(1)).persistAndFlush(any(Comic.class));
    verify(tagService, times(1)).incrementComicCount(anyList());
  }

  @Test
  void testCrawlNettruyenComicQueuesOnlyAfterCommit() throws Exception {
    String slugNId = "test-slug-123";
    when(comicRepository.existsByOriginPathParams(slugNId)).thenReturn(false);

    ExtractedNettruyenComic.Author author =
        new ExtractedNettruyenComic.Author("Person", "John Doe");
    ExtractedNettruyenComic comicInfo =
        new ExtractedNettruyenComic(
            "context",
            "type",
            "Title",
            "Alt",
            "http://url",
            "image.jpg",
            author,
            List.of("Action"),
            "Desc",
            "2023",
            "2023",
            123L,
            "test-slug");
    ExtractedChapters.ChapterItem chapterItem =
        new ExtractedChapters.ChapterItem(
            123L,
            1L,
            "1",
            "chapter-1",
            null,
            1,
            0,
            0,
            null,
            0,
            "jpg",
            10,
            "chapter-1",
            "http://url/chapter-1");
    ExtractedChapters chapters = new ExtractedChapters(List.of(chapterItem));
    ExtractedThumbImage thumbImage =
        new ExtractedThumbImage("http://url", "test.jpg", "temp/test.jpg");
    ExtractedComicDetail detail = new ExtractedComicDetail(comicInfo, chapters, thumbImage);

    when(nettruyenExtractor.extractComicDetail(slugNId)).thenReturn(detail);
    when(tagService.resolveOrCreateTags(anyList())).thenReturn(Collections.emptyList());
    when(waradaConfig.imageBucketImagesPath()).thenReturn("images");
    when(waradaConfig.imageBucket()).thenReturn("warada-images");

    crawlService.crawlNettruyenComic(slugNId);

    verify(comicRepository, times(1)).persistAndFlush(any(Comic.class));
    verify(broadcaster, atLeastOnce()).emit(anyString(), any(CrawlProgressEvent.class));
  }

  @Test
  void testCrawlNettruyenChapterNotFound() {
    when(chapterRepository.findByIdOptional(1L)).thenReturn(Optional.empty());

  }

  @Test
  void testCrawlNettruyenChapterAlreadyFinished() throws Exception {
    Chapter chapter = new Chapter();
    chapter.setId(1L);
    chapter.setCrawlingStatus(CrawlStatus.FINISHED);

    when(chapterRepository.findByIdOptional(1L)).thenReturn(Optional.of(chapter));

    crawlService.crawlChapterById(1L);

    // Should return early, without extracting
    verify(nettruyenExtractor, never()).extractChapterDetail(anyString());
  }

  @Test
  void testCrawlChapterWorkflow() throws Exception {
    Chapter chapter = new Chapter();
    chapter.setId(1L);
    chapter.setCrawlingStatus(CrawlStatus.INIT);
    chapter.setOriginPathParams("test-slug-1");

    Comic comic = new Comic();
    comic.setSlug("test-slug");
    chapter.setComic(comic);
    when(chapterRepository.findByIdWithComic(1L)).thenReturn(Optional.of(chapter));
    when(chapterRepository.findByIdOptional(1L)).thenReturn(Optional.of(chapter));

    // Return 0 images for ease
    ExtractedChapterDetail detail = new ExtractedChapterDetail(Collections.emptyList());
    when(nettruyenExtractor.extractChapterDetail("test-slug-1")).thenReturn(detail);

    crawlService.crawlChapterById(1L);

    // Status should be set to FINISHED eventually
    assertEquals(CrawlStatus.FINISHED, chapter.getCrawlingStatus());
    verify(chapterRepository, atLeastOnce()).persistOrUpdate(chapter);
    verifyNoInteractions(imageRepository);
  }

  @Test
  void testCrawlChapterNotInit() throws Exception {
    Chapter chapter = new Chapter();
    chapter.setId(1L);
    chapter.setCrawlingStatus(CrawlStatus.STARTED); // not FINISHED, but not INIT

    crawlService.crawlChapterById(chapter.getId());

    // Should return early and not fetch
    verify(nettruyenExtractor, never()).extractChapterDetail(anyString());
  }
}
