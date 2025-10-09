package com.github.warada_v2.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrawlNettruyenRequest {
    @NotNull
    @JsonProperty("slug-and-id")
    String slugNId;
}
