package com.github.warada_v2.services;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.github.warada_v2.commons.enums.CrawlStatus;
import com.github.warada_v2.configurations.WaradaConfig;
import com.github.warada_v2.db_entities.Chapter;
import com.github.warada_v2.db_entities.Comic;
import com.github.warada_v2.exceptions.ConflictException;
import com.github.warada_v2.models.ExtractedChapterDetail;
import com.github.warada_v2.models.ExtractedChapters;
import com.github.warada_v2.models.ExtractedComicDetail;
import com.github.warada_v2.models.ExtractedNettruyenComic;
import com.github.warada_v2.models.ExtractedThumbImage;
import com.github.warada_v2.repositories.ChapterRepository;
import com.github.warada_v2.repositories.ComicRepository;
import com.github.warada_v2.repositories.ImageRepository;

import io.minio.MinioClient;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;

class CrawlServiceImplTest {

    @Mock
    ComicRepository comicRepository;

    @Mock
    NettruyenExtractor nettruyenExtractor;

    @Mock
    MinioClient minioClient;

    @Mock
    WaradaConfig waradaConfig;

    @Mock
    ChapterRepository chapterRepository;

    @Mock
    ImageRepository imageRepository;

    @Mock
    TagService tagService;

    @Mock
    ChapterCrawlProducer chapterCrawlProducer;

    @Mock
    CrawlProgressBroadcaster broadcaster;

    @Mock
    TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @InjectMocks
    CrawlServiceImpl crawlService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCrawlNettruyenComicConflict() {
        when(comicRepository.existedBySlugAndComicId("test-slug-123")).thenReturn(true);

        assertThrows(ConflictException.class, () -> crawlService.crawlNettruyenComic("test-slug-123"));
        verify(comicRepository, times(1)).existedBySlugAndComicId("test-slug-123");
        verifyNoMoreInteractions(nettruyenExtractor);
    }

    @Test
    void testCrawlNettruyenComicSuccess() throws Exception {
        String slugNId = "test-slug-123";
        when(comicRepository.existedBySlugAndComicId(slugNId)).thenReturn(false);

        ExtractedNettruyenComic.Author author = new ExtractedNettruyenComic.Author("Person", "John Doe");
        ExtractedNettruyenComic comicInfo = new ExtractedNettruyenComic(
                "context", "type", "Title", "Alt", "http://url", "image.jpg",
                author, List.of("Action"), "Desc", "2023", "2023", 123L, "test-slug"
        );

        ExtractedChapters chapters = new ExtractedChapters(Collections.emptyList());
        ExtractedThumbImage thumbImage = new ExtractedThumbImage("http://url", "test.jpg", "temp/test.jpg");

        ExtractedComicDetail detail = new ExtractedComicDetail(comicInfo, chapters, thumbImage);
        
        when(nettruyenExtractor.extractComicDetail(slugNId)).thenReturn(detail);
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
        when(comicRepository.existedBySlugAndComicId(slugNId)).thenReturn(false);

        ExtractedNettruyenComic.Author author = new ExtractedNettruyenComic.Author("Person", "John Doe");
        ExtractedNettruyenComic comicInfo = new ExtractedNettruyenComic(
                "context", "type", "Title", "Alt", "http://url", "image.jpg",
                author, List.of("Action"), "Desc", "2023", "2023", 123L, "test-slug"
        );
        ExtractedChapters.ChapterItem chapterItem = new ExtractedChapters.ChapterItem(
                123L, 1L, "1", "chapter-1", null, 1, 0, 0, null, 0, "jpg", 10,
                "chapter-1", "http://url/chapter-1"
        );
        ExtractedChapters chapters = new ExtractedChapters(List.of(chapterItem));
        ExtractedThumbImage thumbImage = new ExtractedThumbImage("http://url", "test.jpg", "temp/test.jpg");
        ExtractedComicDetail detail = new ExtractedComicDetail(comicInfo, chapters, thumbImage);

        when(nettruyenExtractor.extractComicDetail(slugNId)).thenReturn(detail);
        when(tagService.resolveOrCreateTags(anyList())).thenReturn(Collections.emptyList());
        when(waradaConfig.imageBucketImagesPath()).thenReturn("images");
        when(waradaConfig.imageBucket()).thenReturn("warada-images");

        crawlService.crawlNettruyenComic(slugNId);

        ArgumentCaptor<Synchronization> captor = ArgumentCaptor.forClass(Synchronization.class);
        verify(transactionSynchronizationRegistry, times(1)).registerInterposedSynchronization(captor.capture());
        verify(chapterCrawlProducer, never()).sendAll(eq(slugNId), anyList());

        captor.getValue().afterCompletion(Status.STATUS_COMMITTED);

        verify(chapterCrawlProducer, times(1)).sendAll(eq(slugNId), anyList());
    }

    @Test
    void testCrawlNettruyenChapterNotFound() {
        when(chapterRepository.findByIdOptional(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> crawlService.crawlNettruyenChapter(1L));
    }

    @Test
    void testCrawlNettruyenChapterAlreadyFinished() {
        Chapter chapter = new Chapter();
        chapter.setId(1L);
        chapter.setCrawlingStatus(CrawlStatus.FINISHED);

        when(chapterRepository.findByIdOptional(1L)).thenReturn(Optional.of(chapter));

        crawlService.crawlNettruyenChapter(1L);

        // Should return early, without extracting
        verify(nettruyenExtractor, never()).extractChapterDetail(anyString());
    }

    @Test
    void testCrawlChapterWorkflow() throws Exception {
        Chapter chapter = new Chapter();
        chapter.setId(1L);
        chapter.setCrawlingStatus(CrawlStatus.INIT);
        chapter.setOriginPathParams("test-slug-1");

        when(chapterRepository.findByIdOptional(1L)).thenReturn(Optional.of(chapter));
        
        // Return 0 images for ease
        ExtractedChapterDetail detail = new ExtractedChapterDetail(Collections.emptyList());
        when(nettruyenExtractor.extractChapterDetail("test-slug-1")).thenReturn(detail);

        crawlService.crawlNettruyenChapter(1L);

        // Status should be set to FINISHED eventually
        assertEquals(CrawlStatus.FINISHED, chapter.getCrawlingStatus());
        verify(chapterRepository, atLeastOnce()).persistOrUpdate(chapter);
        verify(imageRepository, times(1)).persist(anyList());
    }

    @Test
    void testCrawlChapterNotInit() {
        Chapter chapter = new Chapter();
        chapter.setId(1L);
        chapter.setCrawlingStatus(CrawlStatus.STARTED); // not FINISHED, but not INIT

        crawlService.crawlChapter(chapter);

        // Should return early and not fetch
        verify(nettruyenExtractor, never()).extractChapterDetail(anyString());
    }
}
