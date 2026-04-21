package com.github.warada_v2.crawl.models;

import com.fasterxml.jackson.annotation.JsonValue;
import com.github.warada_v2.shared.enums.CrawlStatus;

public record CrawlNettruyenSSEComic(
        CrawlNettruyenSSEType type,
        String message,
        CrawlNettruyenSSEChapter data
) {
    public enum CrawlNettruyenSSEType {
        ADD("add"),
        IGNORE("ignore");

        private final String name;

        CrawlNettruyenSSEType(String name) {
            this.name = name;
        }

        @JsonValue
        public String value() {
            return name;
        }
    }

    public record CrawlNettruyenSSEChapter(
            Long chapterId,
            CrawlStatus status,
            String chapterNumber
    ) {}
}
