package com.github.warada_v2.services;

import com.github.warada_v2.db_entities.Tag;

import java.util.List;

public interface TagService {

    /**
     * Resolve a list of raw tag names (from crawl) into Tag entities.
     * For each name:
     *   - Normalize by trimming + lowercasing
     *   - If a tag with that normalized name exists, reuse it
     *   - Otherwise, create a new Tag
     * Does NOT update comic_count — call {@link #incrementComicCount} after linking.
     */
    List<Tag> resolveOrCreateTags(List<String> rawTagNames);

    /**
     * Increment comic_count by 1 for each given tag.
     */
    void incrementComicCount(List<Tag> tags);

    /**
     * Decrement comic_count by 1 for each given tag (min 0).
     */
    void decrementComicCount(List<Tag> tags);

    /**
     * Get all tags ordered by comic count descending.
     */
    List<Tag> getAllTags();

    /**
     * Find a tag by ID.
     */
    Tag findById(Long id);
}
