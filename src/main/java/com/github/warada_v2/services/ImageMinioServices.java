package com.github.warada_v2.services;

import io.minio.MinioClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
public class ImageMinioServices {

    @ConfigProperty(name = "warada.bucket-name")
    String bucketName;

    public ImageMinioServices() {
        log.info("Creating Minio Client");
        log.info(bucketName);
    }
}
