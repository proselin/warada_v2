package com.github.warada_v2.models;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExtractedThumbImage {
    private String originUrl;
    private String fileName;
    private String objectTemp;
}
