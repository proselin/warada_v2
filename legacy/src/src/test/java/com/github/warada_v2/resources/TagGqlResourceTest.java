package com.github.warada_v2.resources;

import com.github.warada_v2.db_entities.Tag;
import com.github.warada_v2.services.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class TagGqlResourceTest {

    @Mock
    TagService tagService;

    @InjectMocks
    TagGqlResource tagGqlResource;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testTags() {
        Tag tag = new Tag();
        tag.setId(5L);

        when(tagService.getAllTags()).thenReturn(List.of(tag));

        List<Tag> result = tagGqlResource.tags();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getId());
        verify(tagService, times(1)).getAllTags();
    }

    @Test
    void testTag() {
        Tag tag = new Tag();
        tag.setId(10L);

        when(tagService.findById(10L)).thenReturn(tag);

        Tag result = tagGqlResource.tag(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        verify(tagService, times(1)).findById(10L);
    }
}
