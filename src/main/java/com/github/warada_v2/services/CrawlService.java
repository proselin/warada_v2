package com.github.warada_v2.services;

import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.sse.OutboundSseEvent;

public interface CrawlService {
    void crawlNettruyenComic(String slugNId);
    void crawlNettruyenChapter(Long chapterId);

    Multi<OutboundSseEvent> crawlNettruyenSSEChapter(String slugNId);
}
