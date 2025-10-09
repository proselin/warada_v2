package com.github.warada_v2.services;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@JBossLog
@ApplicationScoped
public class ImageServiceImpl implements ImageService {
    @ConfigProperty(name = "warada.bucket-name")
    String bucketName;

    public ImageServiceImpl() {
        log.info("Creating Minio Client");
        log.info(bucketName);
    }
}
