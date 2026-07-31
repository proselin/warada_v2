package com.github.warada_v2.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.github.warada_v2.shared.services.impls.ChapterServiceImpl;
import com.github.warada_v2.shared.entities.Chapter;
import com.github.warada_v2.shared.models.PageRequest;
import com.github.warada_v2.shared.repositories.ChapterRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ChapterServiceImplTest {

  @Mock ChapterRepository chapterRepository;

  @InjectMocks ChapterServiceImpl chapterService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testGetChapterByIdSuccess() {
    Chapter chapter = new Chapter();
    chapter.setId(1L);
    when(chapterRepository.findByIdOptional(1L)).thenReturn(Optional.of(chapter));

    Chapter result = chapterService.getChapterById(1L);

    assertNotNull(result);
    assertEquals(1L, result.getId());
    verify(chapterRepository, times(1)).findByIdOptional(1L);
  }

  @Test
  void testGetChapterByIdNotFound() {
    when(chapterRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

    Chapter result = chapterService.getChapterById(99L);

    assertNull(result);
    verify(chapterRepository, times(1)).findByIdOptional(99L);
  }

  @Test
  void testPageableChapterByComicId() {
    PageRequest request = new PageRequest(0, 10, "id", "asc");
    List<Chapter> chapters = List.of(new Chapter(), new Chapter());

    when(chapterRepository.findByComicId(eq(1L), nullable(Sort.class), any(Page.class)))
        .thenReturn(chapters);

    List<Chapter> result = chapterService.pageableChapterByComicId(1L, request);

    assertEquals(2, result.size());
    verify(chapterRepository, times(1))
        .findByComicId(
            eq(1L), nullable(Sort.class), argThat(page -> page.index == 0 && page.size == 10));
  }

  @Test
  void testGetChapterBySlugSuccess() {
    Chapter chapter = new Chapter();
    chapter.setId(10L);
    when(chapterRepository.findBySlug("some-slug")).thenReturn(Optional.of(chapter));

    Chapter result = chapterService.getChapterBySlug("some-slug");

    assertNotNull(result);
    assertEquals(10L, result.getId());
    verify(chapterRepository, times(1)).findBySlug("some-slug");
  }

  @Test
  void testGetChapterBySlugNotFound() {
    when(chapterRepository.findBySlug("missing-slug")).thenReturn(Optional.empty());

    Chapter result = chapterService.getChapterBySlug("missing-slug");

    assertNull(result);
    verify(chapterRepository, times(1)).findBySlug("missing-slug");
  }
}
