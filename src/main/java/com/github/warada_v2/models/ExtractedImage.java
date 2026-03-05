package com.github.warada_v2.models;


import lombok.Builder;

import java.util.List;

@Builder
public record ExtractedImage(
        List<String> imageUrls,
        String fileName,
        String objectTemp,
        Integer position,
        String imageUrlPulled
) {
    public ExtractedImage withFileNameWithObjectWithImageUrlPulled(String fileName, String object, String imageUrlPulled) {
        return new  ExtractedImage(
                imageUrls(),
                fileName,
                object,
                position(),
                imageUrlPulled
        );
    }
}
