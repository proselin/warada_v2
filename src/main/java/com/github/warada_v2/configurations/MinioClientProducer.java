package com.github.warada_v2.configurations;

import io.minio.MinioClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;

@ApplicationScoped
public class MinioClientProducer {

    @Inject
    MinioConfig config;

    @Produces
    @ApplicationScoped
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(config.endpoint())
                .credentials(config.accessKey(), config.secretKey())
                .build();
    }
}
