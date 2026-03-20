package com.github.warada_v2.services;

import java.util.List;

public interface ComicUpdateQueueService {
    void enqueueComicUpdate(Long comicId);
    void enqueueComicUpdates(List<Long> comicIds);
    Long dequeueComicUpdate();
    long queueSize();
}
