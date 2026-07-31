package com.github.warada_v2.shared.configurations;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "warada")
public interface WaradaConfig {
  String imageBucket();

  NettruyenConfig nettruyen();

  String imageBucketTempPath();

  String imageBucketImagesPath();

  CleanUpTempImages cleanUp();

  interface NettruyenConfig {
    String url();
  }

  interface CleanUpTempImages {
    Integer minAgeMinutes();

    Boolean enabled();
  }
}
