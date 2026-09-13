package com.github.warada_v2.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.warada_v2.shared.services.impls.ComicServiceImpl;
import com.github.warada_v2.shared.entities.Comic;
import com.github.warada_v2.shared.models.PageRequest;
import com.github.warada_v2.shared.repositories.ComicRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import com.github.warada_v2.shared.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ComicServiceImplTest {

  @Mock ComicRepository comicRepository;

  @Mock PanacheQuery<Comic> panacheQuery;

  @Mock PanacheQuery<Comic> panacheQueryPage;

  @InjectMocks ComicServiceImpl comicService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testGetPageableComicsWithNullRequest() {
    List<Comic> expectedComics = List.of(new Comic(), new Comic());

    when(comicRepository.findPageableComics(nullable(Sort.class), any(Page.class)))
        .thenReturn(expectedComics);

    List<Comic> result = comicService.getPageableComics(null);

    assertEquals(2, result.size());
    verify(comicRepository, times(1))
        .findPageableComics(
            nullable(Sort.class), argThat(page -> page.index == 0 && page.size == 20));
  }

  @Test
  void testGetPageableComicsWithValidRequest() {
    PageRequest request = new PageRequest(1, 10, "title", "desc");
    List<Comic> expectedComics = List.of(new Comic());

    when(comicRepository.findPageableComics(nullable(Sort.class), any(Page.class)))
        .thenReturn(expectedComics);

    List<Comic> result = comicService.getPageableComics(request);

    assertEquals(1, result.size());
    verify(comicRepository, times(1)).findPageableComics(nullable(Sort.class), any(Page.class));
  }

  @Test
  void testFindByIdSuccess() {
    Comic comic = new Comic();
    comic.setId(1L);
    when(comicRepository.findByIdOptional(1L)).thenReturn(Optional.of(comic));

    Comic result = comicService.findById(1L);

    assertNotNull(result);
    assertEquals(1L, result.getId());
    verify(comicRepository, times(1)).findByIdOptional(1L);
  }

  @Test
  void testFindByIdNotFound() {
    when(comicRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> comicService.findById(99L));
    verify(comicRepository, times(1)).findByIdOptional(99L);
  }
}
