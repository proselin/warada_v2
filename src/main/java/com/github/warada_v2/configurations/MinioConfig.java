package com.github.warada_v2.configurations;

import io.smallrye.config.ConfigMapping;

import java.net.URL;

@ConfigMapping(prefix = "minio")
public interface MinioConfig {
    URL endpoint();
    String accessKey();
    String secretKey();
    Boolean secure();
}
