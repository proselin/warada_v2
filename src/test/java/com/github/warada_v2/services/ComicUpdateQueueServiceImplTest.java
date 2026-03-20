package com.github.warada_v2.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.list.ListCommands;

class ComicUpdateQueueServiceImplTest {

    @Mock
    RedisDataSource redisDataSource;

    @Mock
    ListCommands<String, String> listCommands;

    ComicUpdateQueueServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisDataSource.list(String.class, String.class)).thenReturn(listCommands);
        service = new ComicUpdateQueueServiceImpl(redisDataSource);
    }

    @Test
    void testEnqueueAndDequeue() {
        when(listCommands.lpop("warada:comic-update-queue")).thenReturn("42");

        Long result = service.dequeueComicUpdate();

        assertEquals(42L, result);
    }

    @Test
    void testDequeueEmpty() {
        when(listCommands.lpop("warada:comic-update-queue")).thenReturn(null);

        Long result = service.dequeueComicUpdate();

        assertNull(result);
    }

    @Test
    void testEnqueueSingle() {
        service.enqueueComicUpdate(10L);

        verify(listCommands).rpush("warada:comic-update-queue", "10");
    }

    @Test
    void testQueueSize() {
        when(listCommands.llen("warada:comic-update-queue")).thenReturn(5L);

        long size = service.queueSize();

        assertEquals(5L, size);
    }
}
