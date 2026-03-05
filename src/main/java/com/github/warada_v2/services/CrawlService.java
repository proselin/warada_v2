package com.github.warada_v2.services;

import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.sse.OutboundSseEvent;

public interface CrawlService {
    void crawlNettruyenComic(String slugNId) throws Exception;
    void crawlNettruyenChapter(Long chapterId) throws Exception;

    Multi<OutboundSseEvent> crawlNettruyenSSEChapter(String slugNId) throws Exception;
}
