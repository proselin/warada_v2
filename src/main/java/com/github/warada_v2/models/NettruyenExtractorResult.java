package com.github.warada_v2.models;

import lombok.Data;

@Data
public class NettruyenExtractorResult {
    private ExtractedNettruyenComicContentDto comic;
    private ExtractedChaptersDto chapters;
    private ExtractedThumbImage thumbImage;
}
