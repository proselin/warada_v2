package com.github.warada_v2.models;

import com.github.warada_v2.commons.enums.CrawlStatus;
import com.github.warada_v2.db_entities.Chapter;
import com.github.warada_v2.db_entities.Comic;
import com.github.warada_v2.db_entities.Image;

import java.time.LocalDateTime;
import java.util.List;

public record ChapterDetailGql(
    Long id,
    LocalDateTime updatedAt,
    LocalDateTime createdAt,
    String chapterNumber,
    String sourceUrl,
    String originPathParams,
    String type,
    Integer position,
    CrawlStatus crawlingStatus,
    Comic comic,
    List<Image> images) {

  public  static ChapterDetailGql fromEntity(Chapter entity) {
    return new ChapterDetailGql(
        entity.getId(),
        entity.getUpdatedAt(),
        entity.getCreatedAt(),
        entity.getChapterNumber(),
        entity.getOriginUrl(),
        entity.getOriginPathParams(),
        entity.getType(),
        entity.getPosition(),
        entity.getCrawlingStatus(),
        entity.getComic(),
        entity.getImages());
  }

  public static List<ChapterDetailGql> fromEntities(List<Chapter> entity) {
    return entity.parallelStream().map(ChapterDetailGql::fromEntity).toList();
  }
}
