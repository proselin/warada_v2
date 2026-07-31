package com.github.warada_v2.api.configurations;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "warada")
public interface WaradaConfig {
    String imageBucket();
    NettruyenConfig nettruyen();

    interface NettruyenConfig {
        String url();
    }

    String imageBucketTempPath();
    String imageBucketImagesPath();
    CleanUpTempImages cleanUp();

    interface CleanUpTempImages {
        Integer minAgeMinutes();
        Boolean enabled();
    }
}
