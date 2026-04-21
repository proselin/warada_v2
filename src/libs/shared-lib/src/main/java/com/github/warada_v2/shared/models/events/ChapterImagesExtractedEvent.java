package com.github.warada_v2.shared.models.events;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonIgnoreProperties(ignoreUnknown = true)
@RegisterForReflection
public record ChapterImagesExtractedEvent(
    Long chapterId,
    String comicSlug,
    String chapterNum,
    List<ImageInfo> images
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    @RegisterForReflection
    public record ImageInfo(
        String fileName,
        String filePath,
        String originUrl,
        int position
    ) {}
}
