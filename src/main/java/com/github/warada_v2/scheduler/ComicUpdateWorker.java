package com.github.warada_v2.scheduler;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

import com.github.warada_v2.commons.enums.CrawlStatus;
import com.github.warada_v2.commons.enums.UpdateSchedule;
import com.github.warada_v2.db_entities.Comic;
import com.github.warada_v2.repositories.ComicRepository;
import com.github.warada_v2.services.ComicUpdateQueueService;
import com.github.warada_v2.services.CrawlService;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
@ApplicationScoped
public class ComicUpdateWorker {

    @Inject
    ComicUpdateQueueService comicUpdateQueueService;

    @Inject
    ComicRepository comicRepository;

    @Inject
    CrawlService crawlService;

    /**
     * Polls the Redis queue every 5 minutes and processes one comic at a time.
     * After crawling, computes the next update date and persists it.
     */
    @Scheduled(every = "5m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    void processUpdateQueue() {
        Long comicId = comicUpdateQueueService.dequeueComicUpdate();
        if (comicId == null) {
            return;
        }

        log.infof("[ComicUpdateWorker] Processing comic ID %d from queue", comicId);

        Comic comic = comicRepository.findByIdOptional(comicId).orElse(null);
        if (comic == null) {
            log.warnf("[ComicUpdateWorker] Comic ID %d not found, skipping", comicId);
            return;
        }

        if (comic.getUpdateOn() == UpdateSchedule.NONE) {
            log.warnf("[ComicUpdateWorker] Comic ID %d has updateOn=NONE, skipping", comicId);
            return;
        }

        try {
            crawlUpdatedChapters(comic);
            comic.setLastUpdateDate(LocalDateTime.now());
            comic.setNextUpdateDate(computeNextUpdateDate(comic));
            comicRepository.persist(comic);
            log.infof("[ComicUpdateWorker] Finished updating comic ID %d, next update: %s",
                    comicId, comic.getNextUpdateDate());
        } catch (Exception e) {
            log.errorf(e, "[ComicUpdateWorker] Failed to process comic ID %d", comicId);
        }
    }

    /**
     * Re-crawls chapters that are in INIT or STARTED status for a given comic.
     */
    private void crawlUpdatedChapters(Comic comic) {
        if (comic.getCrawlingStatus() != CrawlStatus.FINISHED) {
            log.warnf("[ComicUpdateWorker] Comic ID %d crawling not finished, skipping chapter update",
                    comic.getId());
            return;
        }

        // Re-crawl the comic from its origin to pick up new chapters
        try {
            crawlService.crawlNettruyenComic(comic.getOriginPathParams());
        } catch (Exception e) {
            // Comic already exists — expected for re-crawl, SSE handles incremental chapter crawl
            log.debugf("[ComicUpdateWorker] Comic already exists, using SSE chapter crawl for ID %d",
                    comic.getId());
        }

        // Crawl any chapters still in INIT status
        if (comic.getChapters() != null) {
            comic.getChapters().stream()
                    .filter(ch -> ch.getCrawlingStatus() == CrawlStatus.INIT)
                    .forEach(ch -> {
                        try {
                            crawlService.crawlNettruyenChapter(ch.getId());
                        } catch (Exception e) {
                            log.errorf(e, "[ComicUpdateWorker] Failed to crawl chapter %d of comic %d",
                                    ch.getId(), comic.getId());
                        }
                    });
        }
    }

    /**
     * Computes the next update date based on updateOn and fixedTimeToNextUpdate.
     *
     * For WEEK: fixedTimeToNextUpdate = 1 (Monday) to 7 (Sunday)
     * For MONTH: fixedTimeToNextUpdate = 1 to 31 (day of month)
     */
    static LocalDateTime computeNextUpdateDate(Comic comic) {
        LocalDateTime now = LocalDateTime.now();
        UpdateSchedule schedule = comic.getUpdateOn();
        int fixedDay = comic.getFixedTimeToNextUpdate();

        if (schedule == UpdateSchedule.WEEK) {
            // fixedDay: 1=Monday, 2=Tuesday ... 7=Sunday
            int dayValue = Math.max(1, Math.min(7, fixedDay));
            DayOfWeek targetDay = DayOfWeek.of(dayValue);
            LocalDateTime next = now.with(TemporalAdjusters.next(targetDay)).withHour(0).withMinute(0).withSecond(0).withNano(0);
            return next;
        }

        if (schedule == UpdateSchedule.MONTH) {
            // fixedDay: 1-31, day of the month
            int dayOfMonth = Math.max(1, Math.min(31, fixedDay));
            // Adjust to the target day, clamping to month length
            int actualDay = Math.min(dayOfMonth, now.toLocalDate().lengthOfMonth());

            LocalDateTime candidate = now.withDayOfMonth(actualDay).withHour(0).withMinute(0).withSecond(0).withNano(0);
            if (!candidate.isAfter(now)) {
                // Move to next month
                candidate = now.plusMonths(1).withDayOfMonth(1);
                int maxDay = candidate.toLocalDate().lengthOfMonth();
                candidate = candidate.withDayOfMonth(Math.min(dayOfMonth, maxDay))
                        .withHour(0).withMinute(0).withSecond(0).withNano(0);
            }
            return candidate;
        }

        return null;
    }
}
