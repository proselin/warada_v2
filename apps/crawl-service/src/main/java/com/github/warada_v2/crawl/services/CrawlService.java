package com.github.warada_v2.crawl.services;

public interface CrawlService {
    void crawlNettruyenComic(String slugNId) throws Exception;
    void crawlChapterById(Long chapterId) throws Exception;
    void retryFailedChapters(String comicSlug);
}
