package com.github.warada_v2.models;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrawlNettruyenRequest {
    @NotNull
    String url;
}
