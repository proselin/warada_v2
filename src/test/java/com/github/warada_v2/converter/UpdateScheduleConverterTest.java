package com.github.warada_v2.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

import com.github.warada_v2.commons.enums.UpdateSchedule;

class UpdateScheduleConverterTest {

    private final UpdateScheduleConverter converter = new UpdateScheduleConverter();

    @Test
    void testConvertToDatabaseColumn() {
        assertEquals("WEEK", converter.convertToDatabaseColumn(UpdateSchedule.WEEK));
        assertEquals("MONTH", converter.convertToDatabaseColumn(UpdateSchedule.MONTH));
        assertEquals("NONE", converter.convertToDatabaseColumn(UpdateSchedule.NONE));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void testConvertToEntityAttribute() {
        assertEquals(UpdateSchedule.WEEK, converter.convertToEntityAttribute("WEEK"));
        assertEquals(UpdateSchedule.MONTH, converter.convertToEntityAttribute("MONTH"));
        assertEquals(UpdateSchedule.NONE, converter.convertToEntityAttribute("NONE"));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
