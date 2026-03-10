package com.github.warada_v2.models;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ModelsTest {

    @Test
    void testExtractedComicDetail() {
        ExtractedNettruyenComic.Author author = new ExtractedNettruyenComic.Author("url", "name");
        ExtractedNettruyenComic comic = new ExtractedNettruyenComic(
                "ctx", "type", "name", "alt", "url", "image",
                author, List.of("A"), "desc", "pub", "mod", 1L, "slug"
        );
        ExtractedChapters chapters = new ExtractedChapters(Collections.emptyList());
        ExtractedThumbImage thumb = new ExtractedThumbImage("src", "name", "temp");

        ExtractedComicDetail detail = new ExtractedComicDetail(comic, chapters, thumb);

        // test toString, equals, hashCode coverage
        assertNotNull(detail.toString());
        assertEquals(detail, new ExtractedComicDetail(comic, chapters, thumb));
        assertNotNull(detail.hashCode());
        
        assertNotNull(comic.toString());
        assertNotNull(chapters.toString());
        assertNotNull(thumb.toString());
        assertNotNull(author.toString());
    }

    @Test
    void testExtractedChapterDetail() {
        ExtractedChapterDetail detail = new ExtractedChapterDetail(Collections.emptyList());
        assertNotNull(detail.images());
        assertNotNull(detail.toString());
        assertEquals(detail, new ExtractedChapterDetail(Collections.emptyList()));
        assertNotNull(detail.hashCode());
    }


    
    @Test
    void testCrawlRequests() {
        CrawlNettruyenComicRequest comicReq = new CrawlNettruyenComicRequest("slug");
        assertEquals("slug", comicReq.slugNId());
        assertNotNull(comicReq.toString());
        
        CrawlNettruyenChapterRequest chapterReq = new CrawlNettruyenChapterRequest(5L);
        assertEquals(5L, chapterReq.chapterId());
        assertNotNull(chapterReq.toString());
    }

    @Test
    void testCrawlNettruyenSSEComic() {
        CrawlNettruyenSSEComic.CrawlNettruyenSSEChapter chapter = new CrawlNettruyenSSEComic.CrawlNettruyenSSEChapter(
                1L, com.github.warada_v2.commons.enums.CrawlStatus.INIT, "1"
        );
        CrawlNettruyenSSEComic comic = new CrawlNettruyenSSEComic(
                CrawlNettruyenSSEComic.CrawlNettruyenSSEType.ADD,
                "message",
                chapter
        );

        assertEquals(CrawlNettruyenSSEComic.CrawlNettruyenSSEType.ADD, comic.type());
        assertEquals("message", comic.message());
        assertEquals(chapter, comic.data());

        assertEquals(1L, chapter.chapterId());
        assertEquals(com.github.warada_v2.commons.enums.CrawlStatus.INIT, chapter.status());
        assertEquals("1", chapter.chapterNumber());

        assertEquals("add", CrawlNettruyenSSEComic.CrawlNettruyenSSEType.ADD.value());
        assertEquals(CrawlNettruyenSSEComic.CrawlNettruyenSSEType.ADD, CrawlNettruyenSSEComic.CrawlNettruyenSSEType.valueOf("ADD"));
    }

    @Test
    void testExtractedImage() {
        ExtractedImage img = ExtractedImage.builder()
                .imageUrls(List.of("url1", "url2"))
                .fileName("file.jpg")
                .objectTemp("temp")
                .position(1)
                .imageUrlPulled("pulled")
                .build();

        assertEquals(List.of("url1", "url2"), img.imageUrls());
        assertEquals("file.jpg", img.fileName());
        assertEquals("temp", img.objectTemp());
        assertEquals(1, img.position());
        assertEquals("pulled", img.imageUrlPulled());

        ExtractedImage newImg = img.withFileNameWithObjectWithImageUrlPulled("new.jpg", "newObj", "newUrl");
        assertEquals("new.jpg", newImg.fileName());
        assertEquals("newObj", newImg.objectTemp());
        assertEquals("newUrl", newImg.imageUrlPulled());
        assertEquals(1, newImg.position()); // Should be same
    }

    @Test
    void testExtractedChapters() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        ExtractedChapters.ChapterItem item = new ExtractedChapters.ChapterItem(
                1L, 2L, "Chapter 1", "slug", now, 1, 1, 1, now, 1, "type", 1, "path", "url"
        );

        ExtractedChapters chapters = new ExtractedChapters(List.of(item));

        assertEquals(1, chapters.data().size());
        assertEquals(1L, chapters.data().get(0).comicId());
        assertEquals(2L, chapters.data().get(0).chapterId());
        assertEquals("Chapter 1", chapters.data().get(0).chapterName());
        assertEquals("slug", chapters.data().get(0).chapterSlug());
        assertEquals(now, chapters.data().get(0).updatedAt());
        assertEquals(now, chapters.data().get(0).reportedAt());
        assertEquals("type", chapters.data().get(0).imageType());
        
        ExtractedChapters newChapters = chapters.withData(List.of());
        org.junit.jupiter.api.Assertions.assertTrue(newChapters.data().isEmpty());
    }

    @Test
    void testApiResponse() {
        jakarta.ws.rs.core.Response responseMock = org.mockito.Mockito.mock(jakarta.ws.rs.core.Response.class);
        org.mockito.Mockito.when(responseMock.getStatus()).thenReturn(200);
        
        jakarta.ws.rs.core.MultivaluedMap<String, Object> headers = new jakarta.ws.rs.core.MultivaluedHashMap<>();
        headers.putSingle("Content-Type", "application/json");
        org.mockito.Mockito.when(responseMock.getHeaders()).thenReturn(headers);
        org.mockito.Mockito.when(responseMock.readEntity(String.class)).thenReturn("OK");

        ApiResponse<String> apiResponse = ApiResponse.from(responseMock, String.class);

        assertEquals(200, apiResponse.status);
        assertEquals(headers, apiResponse.headers);
        assertEquals("OK", apiResponse.body);
        
        org.mockito.Mockito.verify(responseMock).close();
    }
}
