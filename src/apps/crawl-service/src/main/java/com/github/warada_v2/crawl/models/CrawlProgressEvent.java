package com.github.warada_v2.crawl.models;

import com.fasterxml.jackson.annotation.JsonValue;
import com.github.warada_v2.shared.enums.CrawlStatus;

public record CrawlProgressEvent(
        EventType type,
        String message,
        ChapterProgress data
) {
    public enum EventType {
        PROCESSING("processing"),
        COMPLETED("completed"),
        FAILED("failed"),
        SKIPPED("skipped");

        private final String value;

        EventType(String value) {
            this.value = value;
        }

        @JsonValue
        public String value() {
            return value;
        }
    }

    public record ChapterProgress(
            Long chapterId,
            String chapterNumber,
            CrawlStatus status,
            String comicSlug
    ) {}
}
