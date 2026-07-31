package com.github.warada_v2.crawl.services.impls;

import com.github.warada_v2.crawl.services.CrawlProgressBroadcaster;
import com.github.warada_v2.crawl.models.CrawlProgressEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CrawlProgressBroadcasterImpl implements CrawlProgressBroadcaster {

  private static final Logger log = Logger.getLogger(CrawlProgressBroadcaster.class);

  private final ConcurrentHashMap<
          String, CopyOnWriteArrayList<MultiEmitter<? super CrawlProgressEvent>>>
      emitters = new ConcurrentHashMap<>();
  private final Set<String> activeComics = ConcurrentHashMap.newKeySet();

  public Multi<CrawlProgressEvent> subscribe(String comicSlug) {
    return Multi.createFrom()
        .emitter(
            emitter -> {
              emitters.computeIfAbsent(comicSlug, k -> new CopyOnWriteArrayList<>()).add(emitter);
              log.infof(
                  "SSE subscriber added for comic: %s (total: %d)",
                  comicSlug, emitters.get(comicSlug).size());

              emitter.onTermination(
                  () -> {
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

  public void emit(String comicSlug, CrawlProgressEvent event) {
    var list = emitters.get(comicSlug);
    if (list != null) {
      for (var emitter : list) {
        try {
          emitter.emit(event);
        } catch (Exception e) {
          log.warnf(
              "Failed to emit event to subscriber for comic %s: %s", comicSlug, e.getMessage());
        }
      }
    }
  }

  public boolean markActive(String comicSlug) {
    return activeComics.add(comicSlug);
  }

  public boolean isActive(String comicSlug) {
    return activeComics.contains(comicSlug);
  }

  public void markInactive(String comicSlug) {
    activeComics.remove(comicSlug);
    log.infof("Comic %s marked as inactive (crawl complete)", comicSlug);
  }

  public boolean hasSubscribers(String comicSlug) {
    var list = emitters.get(comicSlug);
    return list != null && !list.isEmpty();
  }
}
