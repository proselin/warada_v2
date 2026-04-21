package com.github.warada_v2.crawl.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import com.github.warada_v2.crawl.configurations.WaradaConfig;

import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.messages.Item;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FileTempClearer {

    private static final Logger log = Logger.getLogger(FileTempClearer.class);

    @Inject MinioClient minio;

    @Inject WaradaConfig waradaConfig;

    private int minAgesMinutes;
    private boolean enabled;
    private String prefix;
    private String bucket;

    @jakarta.annotation.PostConstruct
    void init() {
        minAgesMinutes = waradaConfig.cleanUp().minAgeMinutes();
        enabled = waradaConfig.cleanUp().enabled();
        prefix = waradaConfig.imageBucketTempPath() + "/";
        bucket = waradaConfig.imageBucket();
    }

    @Scheduled(cron = "0 0 0,7,14,19 * * ?", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void cleanTempObjects() {
        if (!enabled) {
            log.debug("FileTempClearer disabled; skipping");
            return;
        }

        final Instant now = Instant.now();
        final Instant cutoff = now.minus(Duration.ofMinutes(minAgesMinutes));
        log.infof("Scanning bucket=%s prefix=%s cutoff=%s", bucket, prefix, cutoff);

        int scanned = 0;
        int candidates = 0;
        int deleted = 0;
        List<String> toDelete = new ArrayList<>();

        try {
            var results = minio.listObjects(
                    ListObjectsArgs.builder().bucket(bucket).prefix(prefix).recursive(true).build());

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
                    minio.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(obj).build());
                    deleted++;
                } catch (Exception e) {
                    log.warnf(e, "Failed to delete %s", obj);
                }
            }

            log.infof("FileTempClearer done: scanned=%d candidates=%d deleted=%d",
                    scanned, candidates, deleted);
        } catch (Exception e) {
            log.error("FileTempClearer fatal error", e);
        }
    }
}
