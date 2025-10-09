package com.github.warada_v2.configurations;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "warada")
public interface WaradaConfig {
    String imageBucket();
    NettruyenConfig nettruyen();

    interface NettruyenConfig {
        String url();
    }

}


