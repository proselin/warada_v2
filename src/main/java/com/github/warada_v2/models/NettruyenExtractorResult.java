package com.github.warada_v2.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonIgnoreProperties(ignoreUnknown = true)
@RegisterForReflection
public record NettruyenExtractorResult(
        ExtractedNettruyenComic comic,
        ExtractedChapters chapters,
        ExtractedThumbImage thumbImage
) {}
