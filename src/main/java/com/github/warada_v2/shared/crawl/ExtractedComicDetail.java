package com.github.warada_v2.shared.crawl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.warada_v2.crawl.models.ExtractedChapters;
import com.github.warada_v2.crawl.models.ExtractedNettruyenComic;
import com.github.warada_v2.crawl.models.ExtractedThumbImage;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonIgnoreProperties(ignoreUnknown = true)
@RegisterForReflection
public record ExtractedComicDetail(
    ExtractedNettruyenComic comic, ExtractedChapters chapters, ExtractedThumbImage thumbImage) {}
