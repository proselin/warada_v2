package com.github.warada_v2.scheduler;

import com.github.warada_v2.configurations.WaradaConfig;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.jbosslog.JBossLog;

import io.minio.MinioClient;
import io.minio.ListObjectsArgs;
import io.minio.RemoveObjectArgs;
import io.minio.messages.Item;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JBossLog
@ApplicationScoped
public class FileTempClearer {

  @Inject MinioClient minio;

  @Inject WaradaConfig waradaConfig;

  private final int MIN_AGES_MINUTES = waradaConfig.cleanUp().minAgeMinutes();
  private final boolean ENABLED = waradaConfig.cleanUp().enabled();
  private final String PREFIX = waradaConfig.imageBucketTempPath() + "/";
  private final String BUCKET = waradaConfig.imageBucket();

  @Scheduled(cron = "0 0 0,7,14,19 * * ?", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
  void cleanTempObjects() {
    if (!ENABLED) {
      log.debug("FileTempClearer disabled; skipping");
      return;
    }

    final Instant now = Instant.now();
    final Instant cutoff = now.minus(Duration.ofMinutes(MIN_AGES_MINUTES));
    log.infof("Scanning bucket=%s prefix=%s cutoff=%s", BUCKET, PREFIX, cutoff);

    int scanned = 0;
    int candidates = 0;
    int deleted = 0;
    List<String> toDelete = new ArrayList<>();

    try {
      var results =
          minio.listObjects(
              ListObjectsArgs.builder().bucket(BUCKET).prefix(PREFIX).recursive(true).build());

      for (var res : results) {
        Item item;
        try {
          item = res.get();
        } catch (Exception e) {
          log.warnf(e, "Failed to read listed item");
          continue;
        }
        if (item.isDir()) continue;
        scanned++;

        Instant lm = item.lastModified() != null ? item.lastModified().toInstant() : null;
        if (lm == null) {
          log.debugf("Skipping %s (no lastModified)", item.objectName());
          continue;
        }
        if (lm.isBefore(cutoff)) {
          toDelete.add(item.objectName());
        }
      }

      candidates = toDelete.size();
      for (String obj : toDelete) {

        try {
          minio.removeObject(RemoveObjectArgs.builder().bucket(BUCKET).object(obj).build());
          deleted++;
        } catch (Exception e) {
          log.warnf(e, "Failed to delete %s", obj);
        }
      }

      log.infof(
          "FileTempClearer done: scanned=%d candidates=%d deleted=%d",
          scanned, candidates, deleted);
    } catch (Exception e) {
      log.error("FileTempClearer fatal error", e);
    }
  }
}
