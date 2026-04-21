package com.github.warada_v2.crawl.services;

import com.github.warada_v2.crawl.models.ExtractedChapterDetail;
import com.github.warada_v2.crawl.models.ExtractedComicDetail;

public interface NettruyenExtractor {
    ExtractedComicDetail extractComicDetail(String slug);
    ExtractedChapterDetail extractChapterDetail(String slug);
}
