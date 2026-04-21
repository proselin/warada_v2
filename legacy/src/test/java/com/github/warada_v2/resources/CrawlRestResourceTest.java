package com.github.warada_v2.resources;

import com.github.warada_v2.models.CrawlNettruyenChapterRequest;
import com.github.warada_v2.models.CrawlNettruyenComicRequest;
import com.github.warada_v2.models.CrawlProgressEvent;
import com.github.warada_v2.services.CrawlProgressBroadcaster;
import com.github.warada_v2.services.CrawlService;
import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class CrawlRestResourceTest {

    @Mock
    CrawlService crawlService;

    @Mock
    CrawlProgressBroadcaster crawlProgressBroadcaster;

    @InjectMocks
    CrawlRestResource crawlRestResource;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCrawlIndividualComic() throws Exception {
        CrawlNettruyenComicRequest request = new CrawlNettruyenComicRequest("test-slug-123");

        Response response = crawlRestResource.crawlIndividualComic(request);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("Success", response.getEntity());
        verify(crawlService, times(1)).crawlNettruyenComic("test-slug-123");
    }

    @Test
    void testCrawlChapter() throws Exception {
        CrawlNettruyenChapterRequest request = new CrawlNettruyenChapterRequest(5L);

        Response response = crawlRestResource.crawlChapter(request);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("Success", response.getEntity());
        verify(crawlService, times(1)).crawlNettruyenChapter(5L);
    }

    @Test
    void testRetryFailedChapters() {
        Response response = crawlRestResource.retryFailedChapters("test-slug-123");

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("Retry queued", response.getEntity());
        verify(crawlService, times(1)).retryFailedChapters("test-slug-123");
    }

    @Test
    void testSubscribeToCrawlProgress() {
        Multi<CrawlProgressEvent> mockMulti = mock(Multi.class);
        when(crawlProgressBroadcaster.subscribe("test-slug-123")).thenReturn(mockMulti);

        Multi<CrawlProgressEvent> result = crawlRestResource.subscribeToCrawlProgress("test-slug-123");

        assertNotNull(result);
        assertEquals(mockMulti, result);
        verify(crawlProgressBroadcaster, times(1)).subscribe("test-slug-123");
    }
}
