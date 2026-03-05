package com.github.warada_v2.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.With;

import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@RegisterForReflection
public record ExtractedChapters(
        @With @JsonProperty("data") List<ChapterItem> data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    @RegisterForReflection
    public record ChapterItem(
            @JsonProperty("comic_id") long comicId,
            @JsonProperty("chapter_id") long chapterId,
            @JsonProperty("chapter_name") String chapterName,
            @JsonProperty("chapter_slug") String chapterSlug,

            // Format: "2025-10-07 23:07:06"
            @JsonProperty("updated_at")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime updatedAt,

            @JsonProperty("chapter_num") int chapterNum,
            @JsonProperty("data_cdn") int dataCdn,
            @JsonProperty("webp") int webp,

            @JsonProperty("reported_at")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime reportedAt,

            @JsonProperty("cdn_sv") int cdnSv,
            @JsonProperty("image_type") String imageType,
            @JsonProperty("image_num") int imageNum,

            // not in the source JSON (your own field)
            @With String originPathParams,
            @With String originUrl
    ) {
    }
}
