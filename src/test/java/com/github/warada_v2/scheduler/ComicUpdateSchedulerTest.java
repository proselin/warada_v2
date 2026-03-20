package com.github.warada_v2.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.github.warada_v2.commons.enums.UpdateSchedule;
import com.github.warada_v2.db_entities.Comic;
import com.github.warada_v2.repositories.ComicRepository;
import com.github.warada_v2.services.ComicUpdateQueueService;

class ComicUpdateSchedulerTest {

    @Mock
    ComicRepository comicRepository;

    @Mock
    ComicUpdateQueueService comicUpdateQueueService;

    @InjectMocks
    ComicUpdateScheduler comicUpdateScheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testScanAndEnqueue_withDueComics() {
        Comic c1 = Comic.builder().id(1L).updateOn(UpdateSchedule.WEEK).fixedTimeToNextUpdate(3)
                .nextUpdateDate(LocalDateTime.now().minusDays(1)).build();
        Comic c2 = Comic.builder().id(2L).updateOn(UpdateSchedule.MONTH).fixedTimeToNextUpdate(15)
                .nextUpdateDate(LocalDateTime.now().minusHours(1)).build();

        when(comicRepository.findComicsDueForUpdate(any(LocalDateTime.class)))
                .thenReturn(List.of(c1, c2));

        comicUpdateScheduler.scanAndEnqueueComicsForUpdate();

        verify(comicUpdateQueueService, times(1)).enqueueComicUpdates(List.of(1L, 2L));
    }

    @Test
    void testScanAndEnqueue_noDueComics() {
        when(comicRepository.findComicsDueForUpdate(any(LocalDateTime.class)))
                .thenReturn(List.of());

        comicUpdateScheduler.scanAndEnqueueComicsForUpdate();

        verify(comicUpdateQueueService, never()).enqueueComicUpdates(any());
    }
}
