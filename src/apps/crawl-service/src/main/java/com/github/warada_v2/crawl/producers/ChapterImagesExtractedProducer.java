package com.github.warada_v2.crawl.producers;

import com.github.warada_v2.shared.models.events.ChapterImagesExtractedEvent;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ChapterImagesExtractedProducer {

    private static final Logger log = Logger.getLogger(ChapterImagesExtractedProducer.class);

    @Inject
    @Channel("chapter-images-extracted-out")
    MutinyEmitter<ChapterImagesExtractedEvent> emitter;

    public void send(ChapterImagesExtractedEvent event) {
        log.infof("Publishing ChapterImagesExtractedEvent chapterId=%d comicSlug=%s", event.chapterId(), event.comicSlug());
        emitter.sendAndAwait(event);
    }
}
