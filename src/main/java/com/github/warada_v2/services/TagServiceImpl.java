package com.github.warada_v2.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.github.warada_v2.db_entities.Tag;
import com.github.warada_v2.repositories.TagRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class TagServiceImpl implements TagService {

    private static final Logger log = Logger.getLogger(TagServiceImpl.class);

    @Inject
    TagRepository tagRepository;

    @Override
    public List<Tag> resolveOrCreateTags(List<String> rawTagNames) {
        if (rawTagNames == null || rawTagNames.isEmpty()) {
            return new ArrayList<>();
        }

        // Normalize all incoming names
        List<String> normalizedNames = rawTagNames.stream()
                .map(name -> name.trim().toLowerCase())
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();

        if (normalizedNames.isEmpty()) {
            return new ArrayList<>();
        }

        // Batch-fetch all existing tags by normalized name
        List<Tag> existingTags = tagRepository.findByNormalizedNameIn(normalizedNames);
        Map<String, Tag> existingMap = existingTags.stream()
                .collect(Collectors.toMap(Tag::getNormalizedName, tag -> tag));

        // Build a map of raw name -> normalized name for display name lookup
        Map<String, String> rawToNormalized = rawTagNames.stream()
                .filter(name -> name != null && !name.trim().isEmpty())
                .collect(Collectors.toMap(
                        name -> name.trim().toLowerCase(),
                        String::trim,
                        (existing, replacement) -> existing  // keep first occurrence as display name
                ));

        List<Tag> result = new ArrayList<>();
        for (String normalized : normalizedNames) {
            Tag tag = existingMap.get(normalized);
            if (tag == null) {
                // Create new tag with original casing as display name
                String displayName = rawToNormalized.getOrDefault(normalized, normalized);
                tag = Tag.builder()
                        .name(displayName)
                        .normalizedName(normalized)
                        .comicCount(0)
                        .build();
                tagRepository.persist(tag);
                log.infof("Created new tag: '%s' (normalized: '%s')", displayName, normalized);
            } else {
                log.infof("Reusing existing tag: '%s' (normalized: '%s')", tag.getName(), normalized);
            }
            result.add(tag);
        }

        return result;
    }

    @Override
    public void incrementComicCount(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) return;
        for (Tag tag : tags) {
            tag.setComicCount(tag.getComicCount() + 1);
            tagRepository.persist(tag);
        }
    }

    @Override
    public void decrementComicCount(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) return;
        for (Tag tag : tags) {
            tag.setComicCount(Math.max(0, tag.getComicCount() - 1));
            tagRepository.persist(tag);
        }
    }

    @Override
    public List<Tag> getAllTags() {
        return tagRepository.findAllOrderByComicCountDesc();
    }

    @Override
    public Tag findById(Long id) {
        return tagRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Tag not found with id: " + id));
    }
}
