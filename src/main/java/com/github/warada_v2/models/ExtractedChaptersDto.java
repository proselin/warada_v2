package com.github.warada_v2.models;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractedChaptersDto {

    @JsonProperty("data")
    public List<ChapterItem> data;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChapterItem {

        @JsonProperty("comic_id")
        public long comicId;

        @JsonProperty("chapter_id")
        public long chapterId;

        @JsonProperty("chapter_name")
        public String chapterName;

        @JsonProperty("chapter_slug")
        public String chapterSlug;

        // Format: "2025-10-07 23:07:06"
        @JsonProperty("updated_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        public LocalDateTime updatedAt;

        @JsonProperty("chapter_num")
        public int chapterNum;

        @JsonProperty("data_cdn")
        public int dataCdn;

        @JsonProperty("webp")
        public int webp;

        @JsonProperty("reported_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        public LocalDateTime reportedAt;

        @JsonProperty("cdn_sv")
        public int cdnSv;

        @JsonProperty("image_type")
        public String imageType;

        @JsonProperty("image_num")
        public int imageNum;

        public String originPathParams;
    }
}
