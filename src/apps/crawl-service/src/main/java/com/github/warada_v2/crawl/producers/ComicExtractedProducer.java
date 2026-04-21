package com.github.warada_v2.crawl.producers;

import com.github.warada_v2.shared.models.events.ComicExtractedEvent;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ComicExtractedProducer {

    private static final Logger log = Logger.getLogger(ComicExtractedProducer.class);

    @Inject
    @Channel("comic-extracted-out")
    MutinyEmitter<ComicExtractedEvent> emitter;

    public void send(ComicExtractedEvent event) {
        log.infof("Publishing ComicExtractedEvent slug=%s", event.slug());
        emitter.sendAndAwait(event);
    }
}
