package com.github.warada_v2.crawl.services;

import com.github.warada_v2.crawl.models.CrawlProgressEvent;
import io.smallrye.mutiny.Multi;

public interface CrawlProgressBroadcaster {
  Multi<CrawlProgressEvent> subscribe(String comicSlug);

  void emit(String comicSlug, CrawlProgressEvent event);

  boolean markActive(String comicSlug);

  boolean isActive(String comicSlug);

  void markInactive(String comicSlug);

  boolean hasSubscribers(String comicSlug);
}
