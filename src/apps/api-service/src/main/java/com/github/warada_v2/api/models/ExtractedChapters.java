package com.github.warada_v2.api.models;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonIgnoreProperties(ignoreUnknown = true)
@RegisterForReflection
public record ExtractedChapters(
        @JsonProperty("data") List<ChapterItem> data
) {
    public ExtractedChapters withData(List<ChapterItem> data) {
        return new ExtractedChapters(data);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @RegisterForReflection
    public record ChapterItem(
            @JsonProperty("comic_id") long comicId,
            @JsonProperty("chapter_id") long chapterId,
            @JsonProperty("chapter_name") String chapterName,
            @JsonProperty("chapter_slug") String chapterSlug,

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

            String originPathParams,
            String originUrl
    ) {
        public ChapterItem withOriginPathParams(String originPathParams) {
            return new ChapterItem(
                    comicId, chapterId, chapterName, chapterSlug, updatedAt,
                    chapterNum, dataCdn, webp, reportedAt, cdnSv, imageType, imageNum,
                    originPathParams, originUrl
            );
        }

        public ChapterItem withOriginUrl(String originUrl) {
            return new ChapterItem(
                    comicId, chapterId, chapterName, chapterSlug, updatedAt,
                    chapterNum, dataCdn, webp, reportedAt, cdnSv, imageType, imageNum,
                    originPathParams, originUrl
            );
        }
    }
}
