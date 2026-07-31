package com.github.warada_v2.api.models;

import com.github.warada_v2.shared.entities.Chapter;
import com.github.warada_v2.shared.entities.Comic;
import com.github.warada_v2.shared.entities.Image;
import com.github.warada_v2.shared.enums.CrawlStatus;
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

  public static ChapterDetailGql fromEntity(Chapter entity) {
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

  public static List<ChapterDetailGql> fromEntities(List<Chapter> entities) {
    return entities.parallelStream().map(ChapterDetailGql::fromEntity).toList();
  }
}
