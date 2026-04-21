package com.github.warada_v2.core.consumers;

import com.github.warada_v2.core.repositories.ChapterRepository;
import com.github.warada_v2.core.repositories.ImageRepository;
import com.github.warada_v2.shared.entities.Chapter;
import com.github.warada_v2.shared.entities.Image;
import com.github.warada_v2.shared.enums.CrawlStatus;
import com.github.warada_v2.shared.enums.ImageType;
import com.github.warada_v2.shared.models.events.ChapterImagesExtractedEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ChapterImagesExtractedConsumer {

    private static final Logger log = Logger.getLogger(ChapterImagesExtractedConsumer.class);

    @Inject ChapterRepository chapterRepository;
    @Inject ImageRepository imageRepository;

    @Incoming("chapter-images-extracted-in")
    @Blocking
    @Transactional
    public void consume(Object payload) {
        ChapterImagesExtractedEvent event = toEvent(payload);
        log.infof("[CORE] Received ChapterImagesExtractedEvent chapterId=%d comicSlug=%s",
                event.chapterId(), event.comicSlug());

        Chapter chapter = chapterRepository.findByIdOptional(event.chapterId()).orElse(null);
        if (chapter == null) {
            log.errorf("[CORE] Chapter not found chapterId=%d", event.chapterId());
            throw new IllegalStateException("Chapter not found: " + event.chapterId());
        }

        // Idempotency: if already FINISHED, skip
        if (chapter.getCrawlingStatus() == CrawlStatus.FINISHED) {
            log.warnf("[CORE] Chapter already finished, skipping chapterId=%d", event.chapterId());
            return;
        }

        if (event.images() != null) {
            for (ChapterImagesExtractedEvent.ImageInfo imgInfo : event.images()) {
                Image img = Image.builder()
                        .chapter(chapter)
                        .fileName(imgInfo.fileName())
                        .filePath(imgInfo.filePath())
                        .originUrl(imgInfo.originUrl())
                        .position(imgInfo.position())
                        .type(ImageType.CHAPTER_IMAGE)
                        .build();
                imageRepository.persist(img);
            }
        }

        chapter.setCrawlingStatus(CrawlStatus.FINISHED);
        chapterRepository.persistOrUpdate(chapter);

        log.infof("[CORE] Chapter finished chapterId=%d images=%d",
                event.chapterId(), event.images() != null ? event.images().size() : 0);
    }

    private ChapterImagesExtractedEvent toEvent(Object payload) {
        if (payload instanceof JsonObject jo) return jo.mapTo(ChapterImagesExtractedEvent.class);
        if (payload instanceof byte[] bytes) return Json.decodeValue(Buffer.buffer(bytes), ChapterImagesExtractedEvent.class);
        if (payload instanceof String s) return Json.decodeValue(s, ChapterImagesExtractedEvent.class);
        throw new IllegalArgumentException("Unsupported payload type: " + (payload == null ? "null" : payload.getClass().getName()));
    }
}
