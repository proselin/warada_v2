package com.github.warada_v2.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.github.warada_v2.models.CrawlNettruyenChapterRequest;
import com.github.warada_v2.models.CrawlNettruyenComicRequest;
import com.github.warada_v2.services.CrawlService;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.OutboundSseEvent;

class CrawlRestResourceTest {

    @Mock
    CrawlService crawlService;

    @InjectMocks
    CrawlRestResource crawlRestResource;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCrawlIndividualComic() {
        CrawlNettruyenComicRequest request = new CrawlNettruyenComicRequest("test-slug-123");

        Uni<Response> uni = crawlRestResource.crawlIndividualComic(request);
        Response response = uni.await().indefinitely();

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("Success", response.getEntity());
        verify(crawlService, times(1)).crawlNettruyenComic("test-slug-123");
    }

    @Test
    void testCrawlChapter() {
        CrawlNettruyenChapterRequest request = new CrawlNettruyenChapterRequest(5L);

        Uni<Response> uni = crawlRestResource.crawlChapter(request);
        Response response = uni.await().indefinitely();

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("Success", response.getEntity());
        verify(crawlService, times(1)).crawlNettruyenChapter(5L);
    }

    @Test
    void testCrawlChapterByComic() {
        Multi<OutboundSseEvent> mockMulti = mock(Multi.class);
        when(crawlService.crawlNettruyenSSEChapter("test-slug-123")).thenReturn(mockMulti);

        Multi<OutboundSseEvent> result = crawlRestResource.crawlChapterByComic("test-slug-123");

        assertNotNull(result);
        assertEquals(mockMulti, result);
        verify(crawlService, times(1)).crawlNettruyenSSEChapter("test-slug-123");
    }
}
