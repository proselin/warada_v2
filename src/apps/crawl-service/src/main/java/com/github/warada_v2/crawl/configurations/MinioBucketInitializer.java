package com.github.warada_v2.crawl.configurations;

import org.jboss.logging.Logger;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class MinioBucketInitializer {

    private static final Logger log = Logger.getLogger(MinioBucketInitializer.class);

    @Inject
    MinioClient minioClient;

    @Inject
    WaradaConfig waradaConfig;

    void onStart(@Observes StartupEvent event) {
        String bucket = waradaConfig.imageBucket();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.infof("Created MinIO bucket: %s", bucket);
                return;
            }
            log.debugf("MinIO bucket already exists: %s", bucket);
        } catch (Exception exception) {
            log.warnf(exception, "MinIO bucket initialization skipped for %s", bucket);
        }
    }
}
