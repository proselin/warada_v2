package com.github.warada_v2.core.services;

import com.github.warada_v2.core.repositories.TagRepository;
import com.github.warada_v2.shared.entities.Tag;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class TagResolverService {

    private static final Logger log = Logger.getLogger(TagResolverService.class);

    @Inject
    TagRepository tagRepository;

    public List<Tag> resolveOrCreateTags(List<String> rawTagNames) {
        if (rawTagNames == null || rawTagNames.isEmpty()) return new ArrayList<>();

        List<String> normalizedNames = rawTagNames.stream()
                .map(name -> name.trim().toLowerCase())
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();

        if (normalizedNames.isEmpty()) return new ArrayList<>();

        List<Tag> existingTags = tagRepository.findByNormalizedNameIn(normalizedNames);
        Map<String, Tag> existingMap = existingTags.stream()
                .collect(Collectors.toMap(Tag::getNormalizedName, t -> t));

        Map<String, String> rawToNormalized = rawTagNames.stream()
                .filter(name -> name != null && !name.trim().isEmpty())
                .collect(Collectors.toMap(
                        name -> name.trim().toLowerCase(),
                        String::trim,
                        (existing, replacement) -> existing
                ));

        List<Tag> result = new ArrayList<>();
        for (String normalized : normalizedNames) {
            Tag tag = existingMap.get(normalized);
            if (tag == null) {
                String displayName = rawToNormalized.getOrDefault(normalized, normalized);
                tag = Tag.builder()
                        .name(displayName)
                        .normalizedName(normalized)
                        .comicCount(0)
                        .build();
                tagRepository.persist(tag);
                log.infof("Created new tag: '%s' (normalized: '%s')", displayName, normalized);
            }
            result.add(tag);
        }
        return result;
    }

    public void incrementComicCount(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) return;
        for (Tag tag : tags) {
            tag.setComicCount(tag.getComicCount() + 1);
            tagRepository.persist(tag);
        }
    }
}
