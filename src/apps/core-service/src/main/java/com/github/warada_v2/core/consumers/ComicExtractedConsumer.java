package com.github.warada_v2.core.consumers;

import com.github.warada_v2.core.producers.ChapterPendingProducer;
import com.github.warada_v2.core.repositories.ChapterRepository;
import com.github.warada_v2.core.repositories.ComicRepository;
import com.github.warada_v2.core.repositories.ImageRepository;
import com.github.warada_v2.core.services.TagResolverService;
import com.github.warada_v2.shared.entities.Chapter;
import com.github.warada_v2.shared.entities.Comic;
import com.github.warada_v2.shared.entities.Image;
import com.github.warada_v2.shared.entities.Tag;
import com.github.warada_v2.shared.enums.CrawlStatus;
import com.github.warada_v2.shared.enums.ImageType;
import com.github.warada_v2.shared.models.events.ChapterPendingEvent;
import com.github.warada_v2.shared.models.events.ComicExtractedEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import java.util.List;

@ApplicationScoped
public class ComicExtractedConsumer {

    private static final Logger log = Logger.getLogger(ComicExtractedConsumer.class);

    @Inject ComicRepository comicRepository;
    @Inject ChapterRepository chapterRepository;
    @Inject ImageRepository imageRepository;
    @Inject TagResolverService tagResolverService;
    @Inject ChapterPendingProducer chapterPendingProducer;

    @Incoming("comic-extracted-in")
    @Blocking
    @Transactional
    public void consume(Object payload) {
        ComicExtractedEvent event = toEvent(payload);
        log.infof("[CORE] Received ComicExtractedEvent slug=%s", event.slug());

        // Idempotency: skip if already persisted
        if (comicRepository.existsByOriginPathParams(event.originPathParams())) {
            log.warnf("[CORE] Comic already persisted, skipping: slug=%s", event.slug());
            return;
        }

        List<Tag> tags = tagResolverService.resolveOrCreateTags(event.tags());

        Comic comic = Comic.builder()
                .slug(event.slug())
                .title(event.title())
                .author(event.author())
                .description(event.description())
                .originId(event.originId())
                .originUrl(event.originUrl())
                .originPathParams(event.originPathParams())
                .status(event.status() != null ? event.status() : "OnGoing")
                .crawlingStatus(CrawlStatus.FINISHED)
                .tags(tags)
                .chapterCount(event.chapters() != null ? event.chapters().size() : 0)
                .build();

        // Persist thumb image
        if (event.thumbImage() != null) {
            var ti = event.thumbImage();
            Image thumb = Image.builder()
                    .fileName(ti.fileName())
                    .filePath(ti.objectTemp()) // objectTemp is already the permanent path by the time this event is published
                    .originUrl(ti.originUrl())
                    .position(0)
                    .type(ImageType.THUMB_IMAGE)
                    .comic(comic)
                    .build();
            comic.setThumbImage(thumb);
        }

        // Persist stub chapters
        if (event.chapters() != null) {
            List<Chapter> chapters = event.chapters().stream()
                    .map(ch -> Chapter.builder()
                            .chapterNumber(ch.chapterName())
                            .position(ch.chapterNum())
                            .originUrl(ch.originUrl())
                            .originPathParams(ch.originPathParams())
                            .crawlingStatus(CrawlStatus.INIT)
                            .comic(comic)
                            .build())
                    .toList();
            comic.setChapters(chapters);
        }

        comicRepository.persistAndFlush(comic);
        tagResolverService.incrementComicCount(tags);

        log.infof("[CORE] Persisted comic id=%d slug=%s with %d chapters",
                comic.getId(), comic.getSlug(),
                comic.getChapters() != null ? comic.getChapters().size() : 0);

        // Publish ChapterPendingEvent for each new chapter
        if (comic.getChapters() != null) {
            for (Chapter ch : comic.getChapters()) {
                chapterPendingProducer.send(new ChapterPendingEvent(
                        ch.getId(),
                        event.slug(),
                        ch.getChapterNumber(),
                        ch.getOriginUrl(),
                        ch.getOriginPathParams()
                ));
            }
        }
    }

    private ComicExtractedEvent toEvent(Object payload) {
        if (payload instanceof JsonObject jo) return jo.mapTo(ComicExtractedEvent.class);
        if (payload instanceof byte[] bytes) return Json.decodeValue(Buffer.buffer(bytes), ComicExtractedEvent.class);
        if (payload instanceof String s) return Json.decodeValue(s, ComicExtractedEvent.class);
        throw new IllegalArgumentException("Unsupported payload type: " + (payload == null ? "null" : payload.getClass().getName()));
    }
}
