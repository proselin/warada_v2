package com.github.warada_v2.converter;

import com.github.warada_v2.commons.enums.CrawlStatus;
import com.github.warada_v2.commons.enums.ImageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConvertersTest {

    @Test
    void testCrawlStatusConverter() {
        CrawlStatusConverter converter = new CrawlStatusConverter();

        // Convert to Database Column
        assertEquals("0000", converter.convertToDatabaseColumn(CrawlStatus.INIT));
        assertEquals("0001", converter.convertToDatabaseColumn(CrawlStatus.STARTED));
        assertEquals("9999", converter.convertToDatabaseColumn(CrawlStatus.FINISHED));
        assertNull(converter.convertToDatabaseColumn(null));

        // Convert to Entity Attribute
        assertEquals(CrawlStatus.INIT, converter.convertToEntityAttribute("0000"));
        assertEquals(CrawlStatus.STARTED, converter.convertToEntityAttribute("0001"));
        assertEquals(CrawlStatus.FINISHED, converter.convertToEntityAttribute("9999"));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void testImageTypeConverter() {
        ImageTypeConverter converter = new ImageTypeConverter();

        // Convert to Database Column
        assertEquals("THUMB", converter.convertToDatabaseColumn(ImageType.THUMB_IMAGE));
        assertEquals("CHAPTER_IMAGE", converter.convertToDatabaseColumn(ImageType.CHAPTER_IMAGE));
        assertNull(converter.convertToDatabaseColumn(null));

        // Convert to Entity Attribute
        assertEquals(ImageType.THUMB_IMAGE, converter.convertToEntityAttribute("THUMB"));
        assertEquals(ImageType.CHAPTER_IMAGE, converter.convertToEntityAttribute("CHAPTER_IMAGE"));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
