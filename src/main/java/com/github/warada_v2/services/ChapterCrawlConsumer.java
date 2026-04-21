package com.github.warada_v2.services;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import com.github.warada_v2.commons.enums.CrawlStatus;
import com.github.warada_v2.db_entities.Chapter;
import com.github.warada_v2.models.ChapterCrawlMessage;
import com.github.warada_v2.models.CrawlProgressEvent;
import com.github.warada_v2.repositories.ChapterRepository;

import io.smallrye.common.annotation.Blocking;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ChapterCrawlConsumer {

    private static final Logger log = Logger.getLogger(ChapterCrawlConsumer.class);

    @Inject
    private ChapterRepository chapterRepository;

    @Inject
    private CrawlService crawlService;

    @Inject
    private CrawlProgressBroadcaster broadcaster;

    @Incoming("chapter-crawl-in")
    @Blocking
    @Transactional
    public void consume(Object payload) {
        ChapterCrawlMessage message = toMessage(payload);
        log.infof("[QUEUE] Processing chapter %d for comic %s", message.chapterId(), message.comicSlug());

        Chapter chapter = chapterRepository.findByIdOptional(message.chapterId()).orElse(null);
        if (chapter == null) {
            String errorMessage = "Chapter %d not found".formatted(message.chapterId());
            log.error(errorMessage);
            emitEvent(message, CrawlProgressEvent.EventType.FAILED, errorMessage, null);
            throw new IllegalStateException(errorMessage);
        }

        if (chapter.getCrawlingStatus() == CrawlStatus.FINISHED) {
            log.infof("[QUEUE] Chapter %d already crawled, skipping", message.chapterId());
            emitEvent(message, CrawlProgressEvent.EventType.SKIPPED, "Already crawled",
                    chapter.getCrawlingStatus());
            checkAndMarkComplete(message.comicSlug());
            return;
        }

        emitEvent(message, CrawlProgressEvent.EventType.PROCESSING, "Processing chapter",
                chapter.getCrawlingStatus());

        try {
            crawlService.crawlChapter(chapter);
            log.infof("[QUEUE] Chapter %d crawled successfully", message.chapterId());
            emitEvent(message, CrawlProgressEvent.EventType.COMPLETED, "Chapter crawled successfully",
                    CrawlStatus.FINISHED);
            checkAndMarkComplete(message.comicSlug());
        } catch (Exception e) {
            log.errorf(e, "[QUEUE] Failed to crawl chapter %d", message.chapterId());
            emitEvent(message, CrawlProgressEvent.EventType.FAILED, "Crawl failed: " + e.getMessage(),
                    chapter.getCrawlingStatus());
            throw e;
        }
    }

    private ChapterCrawlMessage toMessage(Object payload) {
        if (payload instanceof JsonObject jsonObject) {
            return jsonObject.mapTo(ChapterCrawlMessage.class);
        }
        if (payload instanceof byte[] bytes) {
            return Json.decodeValue(Buffer.buffer(bytes), ChapterCrawlMessage.class);
        }
        if (payload instanceof String raw) {
            return Json.decodeValue(raw, ChapterCrawlMessage.class);
        }
        String payloadType = payload == null ? "null" : payload.getClass().getName();
        throw new IllegalArgumentException("Unsupported RabbitMQ payload type: " + payloadType);
    }

    private void emitEvent(ChapterCrawlMessage message, CrawlProgressEvent.EventType type,
                           String eventMessage, CrawlStatus status) {
        var event = new CrawlProgressEvent(
                type,
                eventMessage,
                new CrawlProgressEvent.ChapterProgress(
                        message.chapterId(),
                        null,
                        status,
                        message.comicSlug()
                )
        );
        broadcaster.emit(message.comicSlug(), event);
    }

    /**
     * Check if all chapters for a comic are done (no INIT or STARTED remaining).
     * If so, mark the comic as inactive so retry is allowed again.
     */
    private void checkAndMarkComplete(String comicSlug) {
        long remaining = chapterRepository.countByComicSlugAndStatusIn(comicSlug,
                CrawlStatus.INIT, CrawlStatus.STARTED);
        if (remaining == 0) {
            broadcaster.markInactive(comicSlug);
            log.infof("[QUEUE] All chapters complete for comic %s", comicSlug);
        }
    }
}
