package com.github.warada_v2.api.services;

import com.github.warada_v2.api.models.ExtractedChapterDetail;
import com.github.warada_v2.api.models.ExtractedComicDetail;

public interface NettruyenExtractor {
    ExtractedComicDetail extractComicDetail(String slug);
    ExtractedChapterDetail extractChapterDetail(String slug);
}
