package com.github.warada_v2.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import com.github.warada_v2.api.resources.graphql.ComicGqlResource;
import com.github.warada_v2.shared.entities.Comic;
import com.github.warada_v2.shared.models.PageRequest;
import com.github.warada_v2.shared.services.ComicService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ComicGqlResourceTest {

  @Mock ComicService comicService;

  @InjectMocks
  ComicGqlResource comicGqlResource;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testPing() {
    String pingResult = comicGqlResource.ping();
    assertEquals("Pong", pingResult);
  }

  @Test
  void testComics() {
    PageRequest request = new PageRequest();
    List<Comic> expectedComics = List.of(new Comic());

    when(comicService.getPageableComics(request)).thenReturn(expectedComics);

    List<Comic> result = comicGqlResource.comics(request);

    assertNotNull(result);
    assertEquals(1, result.size());
    verify(comicService, times(1)).getPageableComics(request);
  }

  @Test
  void testComic() {
    Comic comic = new Comic();
    comic.setId(10L);
    when(comicService.findById(10L)).thenReturn(comic);

    Comic result = comicGqlResource.comic(10L);

    assertNotNull(result);
    assertEquals(10L, result.getId());
    verify(comicService, times(1)).findById(10L);
  }
}
