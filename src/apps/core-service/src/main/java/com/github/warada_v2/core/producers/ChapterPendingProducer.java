package com.github.warada_v2.core.producers;

import com.github.warada_v2.shared.models.events.ChapterPendingEvent;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ChapterPendingProducer {

    private static final Logger log = Logger.getLogger(ChapterPendingProducer.class);

    @Inject
    @Channel("chapter-pending-out")
    MutinyEmitter<ChapterPendingEvent> emitter;

    public void send(ChapterPendingEvent event) {
        log.infof("Publishing ChapterPendingEvent chapterId=%d comicSlug=%s", event.chapterId(), event.comicSlug());
        emitter.sendAndAwait(event);
    }
}
