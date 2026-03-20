package com.github.warada_v2.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import com.github.warada_v2.repositories.ComicRepository;
import com.github.warada_v2.services.ComicUpdateQueueService;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
@ApplicationScoped
public class ComicUpdateScheduler {

    @Inject
    ComicRepository comicRepository;

    @Inject
    ComicUpdateQueueService comicUpdateQueueService;

    /**
     * Runs every hour. Scans all comics where next_update_date <= now and update_on != NONE,
     * then pushes their IDs into the Redis queue for the worker to process.
     */
    @Scheduled(cron = "0 0 * * * ?", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    void scanAndEnqueueComicsForUpdate() {
        LocalDateTime now = LocalDateTime.now();
        log.infof("[ComicUpdateScheduler] Scanning comics due for update at %s", now);

        List<Long> comicIds = comicRepository.findComicsDueForUpdate(now)
                .stream()
                .map(comic -> comic.getId())
                .toList();

        if (comicIds.isEmpty()) {
            log.info("[ComicUpdateScheduler] No comics due for update");
            return;
        }

        comicUpdateQueueService.enqueueComicUpdates(comicIds);
        log.infof("[ComicUpdateScheduler] Enqueued %d comics for update", comicIds.size());
    }
}
