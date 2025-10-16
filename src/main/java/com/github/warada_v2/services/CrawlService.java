package com.github.warada_v2.services;

public interface CrawlService {
    void crawlNettruyenComic(String slugNId) throws Exception;
    void crawlNettruyenChapter(Long chapterId) throws Exception;
}
