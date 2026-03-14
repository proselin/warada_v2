package com.github.warada_v2.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateChapterRequest(
        @JsonProperty("chapter_number") String chapterNumber,
        String type,
        Integer position
) {
}
