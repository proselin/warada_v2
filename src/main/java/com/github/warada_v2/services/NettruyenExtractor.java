package com.github.warada_v2.services;

import com.github.warada_v2.models.NettruyenExtractorResult;

public interface NettruyenExtractor {
    NettruyenExtractorResult extract(String url);
}
