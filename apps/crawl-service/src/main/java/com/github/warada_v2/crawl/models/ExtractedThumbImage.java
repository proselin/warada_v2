package com.github.warada_v2.crawl.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonIgnoreProperties(ignoreUnknown = true)
@RegisterForReflection
public record ExtractedThumbImage(
        String originUrl,
        String fileName,
        String objectTemp
) {}
