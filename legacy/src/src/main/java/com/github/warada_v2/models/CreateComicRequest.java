package com.github.warada_v2.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateComicRequest(
        String title,
        String description,
        String author,
        String status,
        String slug,
        @JsonProperty("origin_id") String originId,
        @JsonProperty("origin_url") String originUrl,
        @JsonProperty("origin_path_params") String originPathParams,
        List<String> tags
) {
}
