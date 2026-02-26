package com.github.warada_v2.services;

import com.github.warada_v2.models.CrawlNettruyenSSEComic;
import io.smallrye.mutiny.Multi;

public interface CrawlService {
    void crawlNettruyenComic(String slugNId) throws Exception;
    void crawlNettruyenChapter(Long chapterId) throws Exception;

    Multi<CrawlNettruyenSSEComic> crawlNettruyenSSEChapter(String slugNId) throws Exception;
}
