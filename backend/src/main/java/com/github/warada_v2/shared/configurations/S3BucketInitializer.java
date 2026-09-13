package com.github.warada_v2.shared.configurations;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class S3BucketInitializer {

  private static final Logger log = Logger.getLogger(S3BucketInitializer.class);

  @Inject MinioClient minioClient;

  @Inject WaradaConfig waradaConfig;

  void onStart(@Observes StartupEvent event) {
    String bucket = waradaConfig.imageBucket();
    try {
      boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
      if (!exists) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        log.infof("Created S3 bucket: %s", bucket);
        return;
      }
      log.debugf("S3 bucket already exists: %s", bucket);
    } catch (Exception exception) {
      log.warnf(exception, "S3 bucket initialization skipped for %s", bucket);
    }
  }
}
