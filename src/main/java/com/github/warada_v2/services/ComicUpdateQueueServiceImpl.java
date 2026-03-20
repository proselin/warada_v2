package com.github.warada_v2.services;

import java.util.List;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.list.ListCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
@ApplicationScoped
public class ComicUpdateQueueServiceImpl implements ComicUpdateQueueService {

    private static final String QUEUE_KEY = "warada:comic-update-queue";

    private final ListCommands<String, String> listCommands;

    @Inject
    public ComicUpdateQueueServiceImpl(RedisDataSource redisDataSource) {
        this.listCommands = redisDataSource.list(String.class, String.class);
    }

    @Override
    public void enqueueComicUpdate(Long comicId) {
        listCommands.rpush(QUEUE_KEY, comicId.toString());
        log.debugf("Enqueued comic ID %d for update", comicId);
    }

    @Override
    public void enqueueComicUpdates(List<Long> comicIds) {
        if (comicIds.isEmpty()) return;
        String[] ids = comicIds.stream().map(String::valueOf).toArray(String[]::new);
        listCommands.rpush(QUEUE_KEY, ids);
        log.infof("Enqueued %d comics for update", comicIds.size());
    }

    @Override
    public Long dequeueComicUpdate() {
        String value = listCommands.lpop(QUEUE_KEY);
        if (value == null) return null;
        return Long.parseLong(value);
    }

    @Override
    public long queueSize() {
        return listCommands.llen(QUEUE_KEY);
    }
}
