package com.github.warada_v2.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.warada_v2.shared.services.impls.TagServiceImpl;
import com.github.warada_v2.shared.entities.Tag;
import com.github.warada_v2.shared.repositories.TagRepository;
import jakarta.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class TagServiceImplTest {

  @Mock TagRepository tagRepository;

  @InjectMocks
  TagServiceImpl tagService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testResolveOrCreateTagsWithEmptyInput() {
    List<Tag> resultEmpty = tagService.resolveOrCreateTags(Collections.emptyList());
    assertTrue(resultEmpty.isEmpty());

    List<Tag> resultNull = tagService.resolveOrCreateTags(null);
    assertTrue(resultNull.isEmpty());
  }

  @Test
  void testResolveOrCreateTagsMixed() {
    List<String> rawNames = List.of(" Action ", "ACTION", "comedy", "");

    Tag existingActionTag =
        Tag.builder().name("Action").normalizedName("action").comicCount(5).build();
    when(tagRepository.findByNormalizedNameIn(anyList())).thenReturn(List.of(existingActionTag));

    List<Tag> result = tagService.resolveOrCreateTags(rawNames);

    // " Action " and "ACTION" merge to "action"
    // "comedy" stays "comedy"
    // "" is ignored
    assertEquals(2, result.size());

    // Ensure the existing action tag is reused
    assertTrue(result.contains(existingActionTag));

    // Ensure persist is called for the new "comedy" tag
    verify(tagRepository, times(1)).persist(any(Tag.class));
  }

  @Test
  void testIncrementComicCount() {
    Tag tag1 = Tag.builder().comicCount(1).build();
    Tag tag2 = Tag.builder().comicCount(5).build();

    tagService.incrementComicCount(List.of(tag1, tag2));

    assertEquals(2, tag1.getComicCount());
    assertEquals(6, tag2.getComicCount());
    verify(tagRepository, times(2)).persist(any(Tag.class));
  }

//  @Test
//  void testDecrementComicCount() {
//    Tag tag1 = Tag.builder().comicCount(1).build();
//    Tag tag2 = Tag.builder().comicCount(5).build();
//
//    tagService.decrementComicCount(List.of(tag1, tag2));
//
//    assertEquals(0, tag1.getComicCount());
//    assertEquals(4, tag2.getComicCount());
//    verify(tagRepository, times(2)).persist(any(Tag.class));
//  }

//  @Test
//  void testDecrementComicCountBelowZero() {
//    Tag tag1 = Tag.builder().comicCount(0).build();
//
//    tagService.decrementComicCount(List.of(tag1));
//
//    assertEquals(0, tag1.getComicCount());
//  }

  @Test
  void testGetAllTags() {
    List<Tag> tags = List.of(new Tag(), new Tag());
    when(tagRepository.findAllOrderByComicCountDesc()).thenReturn(tags);

    List<Tag> result = tagService.getAllTags();

    assertEquals(2, result.size());
    verify(tagRepository, times(1)).findAllOrderByComicCountDesc();
  }

  @Test
  void testFindByIdSuccess() {
    Tag tag = new Tag();
    when(tagRepository.findByIdOptional(1L)).thenReturn(Optional.of(tag));

    Tag result = tagService.findById(1L);

    assertNotNull(result);
    verify(tagRepository, times(1)).findByIdOptional(1L);
  }

  @Test
  void testFindByIdNotFound() {
    when(tagRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> tagService.findById(99L));
  }
}
