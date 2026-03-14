package com.github.warada_v2.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateChapterRequest(
        @JsonProperty("comic_id") Long comicId,
        @JsonProperty("chapter_number") String chapterNumber,
        String type,
        Integer position,
        @JsonProperty("origin_url") String originUrl,
        @JsonProperty("origin_path_params") String originPathParams
) {
}
