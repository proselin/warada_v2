package com.github.warada_v2.crawl.consumers;

import com.github.warada_v2.crawl.models.CrawlProgressEvent;
import com.github.warada_v2.crawl.services.CrawlProgressBroadcaster;
import com.github.warada_v2.crawl.services.CrawlServiceImpl;
import com.github.warada_v2.shared.models.events.ChapterPendingEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ChapterPendingConsumer {

    private static final Logger log = Logger.getLogger(ChapterPendingConsumer.class);

    @Inject CrawlServiceImpl crawlService;
    @Inject CrawlProgressBroadcaster broadcaster;

    @Incoming("chapter-pending-in")
    @Blocking
    public void consume(Object payload) {
        ChapterPendingEvent event = toEvent(payload);
        log.infof("[CRAWL] Processing ChapterPendingEvent chapterId=%d comicSlug=%s", event.chapterId(), event.comicSlug());

        broadcaster.emit(event.comicSlug(), new CrawlProgressEvent(
                CrawlProgressEvent.EventType.PROCESSING,
                "Processing chapter " + event.chapterNum(),
                new CrawlProgressEvent.ChapterProgress(event.chapterId(), event.chapterNum(), null, event.comicSlug())
        ));

        try {
            crawlService.crawlChapterFromEvent(event);
            broadcaster.emit(event.comicSlug(), new CrawlProgressEvent(
                    CrawlProgressEvent.EventType.COMPLETED,
                    "Chapter crawled successfully",
                    new CrawlProgressEvent.ChapterProgress(event.chapterId(), event.chapterNum(), null, event.comicSlug())
            ));
        } catch (Exception e) {
            log.errorf(e, "[CRAWL] Failed to crawl chapter chapterId=%d", event.chapterId());
            broadcaster.emit(event.comicSlug(), new CrawlProgressEvent(
                    CrawlProgressEvent.EventType.FAILED,
                    "Failed: " + e.getMessage(),
                    new CrawlProgressEvent.ChapterProgress(event.chapterId(), event.chapterNum(), null, event.comicSlug())
            ));
            throw new RuntimeException(e);
        }
    }

    private ChapterPendingEvent toEvent(Object payload) {
        if (payload instanceof JsonObject jo) return jo.mapTo(ChapterPendingEvent.class);
        if (payload instanceof byte[] bytes) return Json.decodeValue(Buffer.buffer(bytes), ChapterPendingEvent.class);
        if (payload instanceof String s) return Json.decodeValue(s, ChapterPendingEvent.class);
        throw new IllegalArgumentException("Unsupported payload type: " + (payload == null ? "null" : payload.getClass().getName()));
    }
}
