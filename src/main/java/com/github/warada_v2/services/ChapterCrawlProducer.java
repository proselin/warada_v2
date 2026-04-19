package com.github.warada_v2.services;

import java.util.List;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.logging.Logger;

import com.github.warada_v2.models.ChapterCrawlMessage;

import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ChapterCrawlProducer {

    private static final Logger log = Logger.getLogger(ChapterCrawlProducer.class);

    @Inject
    @Channel("chapter-crawl-out")
    MutinyEmitter<ChapterCrawlMessage> emitter;

    @Inject
    CrawlProgressBroadcaster broadcaster;

    /**
     * Send a single chapter crawl message to the queue.
     */
    public void send(ChapterCrawlMessage message) {
        log.infof("Sending chapter %d for comic %s to crawl queue", message.chapterId(), message.comicSlug());
        emitter.sendAndAwait(message);
    }

    /**
     * Send all chapters for a comic to the queue.
     * Marks the comic as active to block duplicate requests.
     */
    public void sendAll(String comicSlug, List<ChapterCrawlMessage> messages) {
        if (messages.isEmpty()) {
            log.infof("No chapters to queue for comic %s", comicSlug);
            return;
        }

        broadcaster.markActive(comicSlug);
        log.infof("Queuing %d chapters for comic %s", messages.size(), comicSlug);

        for (var message : messages) {
            emitter.sendAndAwait(message);
        }

        log.infof("All %d chapters queued for comic %s", messages.size(), comicSlug);
    }
}
