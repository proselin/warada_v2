package com.github.warada_v2.crawl.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

public record CrawlNettruyenChapterRequest(@NotNull @JsonProperty("chapter_id") Long chapterId) {}
