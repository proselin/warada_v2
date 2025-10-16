package com.github.warada_v2.services;

import com.github.warada_v2.models.ExtractedChapterDetail;
import com.github.warada_v2.models.ExtractedComicDetail;

public interface NettruyenExtractor {
    ExtractedComicDetail extractComicDetail(String slug);
    ExtractedChapterDetail extractChapterDetail(String slug);
}
