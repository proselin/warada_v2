package com.github.warada_v2.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.warada_v2.crawl.services.impls.NettruyenExtractorImpl;
import com.github.warada_v2.crawl.client.NettruyenRestService;
import com.github.warada_v2.crawl.models.ExtractedChapters;
import com.github.warada_v2.crawl.models.ExtractedNettruyenComic;
import com.github.warada_v2.shared.configurations.WaradaConfig;
import io.minio.MinioClient;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class NettruyenExtractorImplTest {

  @Mock NettruyenRestService nettruyenRestService;

  @Mock MinioClient minioClient;

  @Mock WaradaConfig waradaConfig;

  @Mock WaradaConfig.NettruyenConfig nettruyenConfig;

  @InjectMocks NettruyenExtractorImpl extractor;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(waradaConfig.nettruyen()).thenReturn(nettruyenConfig);
    when(nettruyenConfig.url()).thenReturn("http://nettruyen.test");
  }

  @Test
  void testExtractComicHtmlContentSuccess() {
    String html =
        """
            <html>
                <script type="application/ld+json">
                {
                  "@context": "https://schema.org",
                  "@type": "ComicSeries",
                  "name": "One Piece",
                  "author": {
                    "@type": "Person",
                    "name": "Oda Eiichiro"
                  },
                  "genre": [
                    "Action",
                    "Adventure"
                  ],
                  "url": "http://nettruyen.test/truyen-tranh/one-piece-1234"
                }
                </script>
                <script>
                    var gOpts = {};
                    gOpts.comicId = 1234;
                    gOpts.comicSlug = 'one-piece';
                </script>
            </html>
        """;

    ExtractedNettruyenComic result = extractor.extractComicHtmlContent(html);

    assertNotNull(result);
    assertEquals("One Piece", result.name());
    assertEquals(1234L, result.comicId());
    assertEquals("one-piece", result.slug());
  }

  @Test
  void testExtractComicHtmlContentMalformedJsonLd() {
    String html =
        """
            <html>
                <script type="application/ld+json">
                {
                  invalid json
                }
                </script>
                <script>
                    var gOpts = {};
                    gOpts.comicId = 1234;
                    gOpts.comicSlug = 'one-piece';
                </script>
            </html>
        """;

    assertThrows(RuntimeException.class, () -> extractor.extractComicHtmlContent(html));
  }

  @Test
  void testGetChapterListEmpty() {
    // Mock rest service response
    ExtractedChapters mockChapters = new ExtractedChapters(Collections.emptyList());
    Response mockResponse = mock(Response.class);
    when(mockResponse.readEntity(ExtractedChapters.class)).thenReturn(mockChapters);
    when(nettruyenRestService.getComicChapterList(eq("test-slug"), eq(1234L)))
        .thenReturn(mockResponse);

    ExtractedChapters result = extractor.getChapterList(1234L, "test-slug");

    assertNotNull(result);
    assertTrue(result.data().isEmpty());
  }
}
