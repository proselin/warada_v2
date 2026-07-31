package com.github.warada_v2.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

public record CrawlNettruyenComicRequest(@NotNull @JsonProperty("slug-and-id") String slugNId) {}
