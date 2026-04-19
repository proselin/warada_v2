package com.github.warada_v2.services;

import com.github.warada_v2.db_entities.Chapter;

public interface CrawlService {
    void crawlNettruyenComic(String slugNId) throws Exception;
    void crawlNettruyenChapter(Long chapterId) throws Exception;
    void crawlChapter(Chapter chapter);
    void retryFailedChapters(String comicSlug);
}
