package com.github.warada_v2.scheduler;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.github.warada_v2.commons.enums.UpdateSchedule;
import com.github.warada_v2.db_entities.Comic;

class ComicUpdateWorkerTest {

    @Test
    void testComputeNextUpdateDate_week_returnsNextTargetDay() {
        Comic comic = Comic.builder()
                .updateOn(UpdateSchedule.WEEK)
                .fixedTimeToNextUpdate(3) // Wednesday
                .build();

        LocalDateTime result = ComicUpdateWorker.computeNextUpdateDate(comic);

        assertNotNull(result);
        assertEquals(DayOfWeek.WEDNESDAY, result.getDayOfWeek());
        assertTrue(result.isAfter(LocalDateTime.now()));
        assertEquals(0, result.getHour());
        assertEquals(0, result.getMinute());
    }

    @Test
    void testComputeNextUpdateDate_month_returnsNextDay15() {
        Comic comic = Comic.builder()
                .updateOn(UpdateSchedule.MONTH)
                .fixedTimeToNextUpdate(15)
                .build();

        LocalDateTime result = ComicUpdateWorker.computeNextUpdateDate(comic);

        assertNotNull(result);
        assertTrue(result.isAfter(LocalDateTime.now()));
        assertTrue(result.getDayOfMonth() <= 15);
        assertEquals(0, result.getHour());
    }

    @Test
    void testComputeNextUpdateDate_none_returnsNull() {
        Comic comic = Comic.builder()
                .updateOn(UpdateSchedule.NONE)
                .fixedTimeToNextUpdate(0)
                .build();

        LocalDateTime result = ComicUpdateWorker.computeNextUpdateDate(comic);

        assertNull(result);
    }

    @Test
    void testComputeNextUpdateDate_week_clampsDayRange() {
        Comic comic = Comic.builder()
                .updateOn(UpdateSchedule.WEEK)
                .fixedTimeToNextUpdate(99) // out of range, should clamp to 7 (Sunday)
                .build();

        LocalDateTime result = ComicUpdateWorker.computeNextUpdateDate(comic);

        assertNotNull(result);
        assertEquals(DayOfWeek.SUNDAY, result.getDayOfWeek());
    }

    @Test
    void testComputeNextUpdateDate_month_clampsDay31() {
        Comic comic = Comic.builder()
                .updateOn(UpdateSchedule.MONTH)
                .fixedTimeToNextUpdate(31)
                .build();

        LocalDateTime result = ComicUpdateWorker.computeNextUpdateDate(comic);

        assertNotNull(result);
        assertTrue(result.isAfter(LocalDateTime.now()));
    }
}
