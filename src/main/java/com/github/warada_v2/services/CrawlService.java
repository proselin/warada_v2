package com.github.warada_v2.services;

import com.github.warada_v2.db_entities.Comic;
import com.github.warada_v2.exceptions.ConflictException;
import com.github.warada_v2.repositories.ComicRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class CrawlService {

    @Inject
    ComicRepository comicRepository;

    @Inject
    NettruyenExtractor nettruyenExtractor;

    /**
     * @param url the link of crawling nettruyen
     * @return
     */
    public Comic crawlNettruyenComic(String url) {
        log.info("START Crawling Nettruyen Comic from {}", url);
        // Check is comic is existed or not
        if(comicRepository.existedByOriginUrl(url)) {
            throw new ConflictException("Crawling comic from " + url + " already exists");
        }
        // Do crawl logic here
        nettruyenExtractor.extract(url);
        log.info("DONE Crawling Nettruyen Comic from {}", url);
        return null;
    }
}
