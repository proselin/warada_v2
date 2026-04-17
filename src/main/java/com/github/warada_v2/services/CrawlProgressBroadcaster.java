package com.github.warada_v2.services;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.github.warada_v2.models.CrawlProgressEvent;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
@ApplicationScoped
public class CrawlProgressBroadcaster {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<MultiEmitter<? super CrawlProgressEvent>>> emitters = new ConcurrentHashMap<>();
    private final Set<String> activeComics = ConcurrentHashMap.newKeySet();

    /**
     * Subscribe to crawl progress events for a specific comic.
     * Only receives events emitted after subscription (no replay).
     */
    public Multi<CrawlProgressEvent> subscribe(String comicSlug) {
        return Multi.createFrom().emitter(emitter -> {
            emitters.computeIfAbsent(comicSlug, k -> new CopyOnWriteArrayList<>()).add(emitter);
            log.infof("SSE subscriber added for comic: %s (total: %d)", comicSlug,
                    emitters.get(comicSlug).size());

            emitter.onTermination(() -> {
                var list = emitters.get(comicSlug);
                if (list != null) {
                    list.remove(emitter);
                    if (list.isEmpty()) {
                        emitters.remove(comicSlug);
                    }
                }
                log.infof("SSE subscriber removed for comic: %s", comicSlug);
            });
        });
    }

    /**
     * Emit a progress event to all subscribers of a specific comic.
     */
    public void emit(String comicSlug, CrawlProgressEvent event) {
        var list = emitters.get(comicSlug);
        if (list != null) {
            for (var emitter : list) {
                try {
                    emitter.emit(event);
                } catch (Exception e) {
                    log.warnf("Failed to emit event to subscriber for comic %s: %s", comicSlug, e.getMessage());
                }
            }
        }
    }

    /**
     * Mark a comic as having an active crawl job in the queue.
     * Returns false if already active (used to block duplicate jobs).
     */
    public boolean markActive(String comicSlug) {
        return activeComics.add(comicSlug);
    }

    /**
     * Check if a comic has an active crawl job.
     */
    public boolean isActive(String comicSlug) {
        return activeComics.contains(comicSlug);
    }

    /**
     * Mark a comic as no longer having active crawl jobs.
     */
    public void markInactive(String comicSlug) {
        activeComics.remove(comicSlug);
        log.infof("Comic %s marked as inactive (crawl complete)", comicSlug);
    }

    /**
     * Check if any subscribers exist for a comic.
     */
    public boolean hasSubscribers(String comicSlug) {
        var list = emitters.get(comicSlug);
        return list != null && !list.isEmpty();
    }
}
