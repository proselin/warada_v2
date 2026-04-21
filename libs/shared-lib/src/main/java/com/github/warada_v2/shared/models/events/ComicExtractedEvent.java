package com.github.warada_v2.shared.models.events;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonIgnoreProperties(ignoreUnknown = true)
@RegisterForReflection
public record ComicExtractedEvent(
    String slug,
    String title,
    String author,
    String description,
    String originId,
    String originUrl,
    String originPathParams,
    String status,
    List<String> tags,
    ThumbImageInfo thumbImage,
    List<ChapterInfo> chapters
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    @RegisterForReflection
    public record ThumbImageInfo(
        String originUrl,
        String fileName,
        String objectTemp
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @RegisterForReflection
    public record ChapterInfo(
        String chapterName,
        String originPathParams,
        String originUrl,
        int chapterNum
    ) {}
}
