package com.github.warada_v2.shared.models.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonIgnoreProperties(ignoreUnknown = true)
@RegisterForReflection
public record ChapterPendingEvent(
    Long chapterId,
    String comicSlug,
    String chapterNum,
    String originUrl,
    String originPathParams) {}
