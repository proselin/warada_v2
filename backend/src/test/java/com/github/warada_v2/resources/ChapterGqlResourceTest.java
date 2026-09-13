package com.github.warada_v2.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import com.github.warada_v2.api.models.ChapterDetailGql;
import com.github.warada_v2.api.resources.graphql.ChapterGqlResource;
import com.github.warada_v2.shared.services.ChapterService;
import com.github.warada_v2.shared.entities.Chapter;
import com.github.warada_v2.shared.models.PageRequest;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ChapterGqlResourceTest {

  @Mock ChapterService chapterService;

  @InjectMocks ChapterGqlResource chapterGqlResource;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testGetChapterById() {
    Chapter chapter = new Chapter();
    chapter.setId(1L);
    chapter.setImages(Collections.emptyList());

    when(chapterService.getChapterById(1L)).thenReturn(chapter);

    ChapterDetailGql result = chapterGqlResource.getChapterById(1L);

    assertNotNull(result);
    assertEquals(1L, result.id());
    verify(chapterService, times(1)).getChapterById(1L);
  }

  @Test
  void testGetChapterBySlug() {
    Chapter chapter = new Chapter();
    chapter.setId(2L);
    chapter.setImages(Collections.emptyList());

    when(chapterService.getChapterBySlug("slug")).thenReturn(chapter);

    ChapterDetailGql result = chapterGqlResource.getChapterBySlug("slug");

    assertNotNull(result);
    assertEquals(2L, result.id());
    verify(chapterService, times(1)).getChapterBySlug("slug");
  }

  @Test
  void testPageableChapterByComicId() {
    PageRequest request = new PageRequest();
    Chapter chapter = new Chapter();
    chapter.setId(10L);
    chapter.setImages(Collections.emptyList());

    when(chapterService.pageableChapterByComicId(10L, request)).thenReturn(List.of(chapter));

    List<ChapterDetailGql> result = chapterGqlResource.pageableChapterByComicId(10L, request);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(10L, result.get(0).id());
    verify(chapterService, times(1)).pageableChapterByComicId(10L, request);
  }
}
