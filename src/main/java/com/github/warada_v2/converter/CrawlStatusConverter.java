package com.github.warada_v2.converter;

import com.github.warada_v2.commons.enums.CrawlStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CrawlStatusConverter implements AttributeConverter<CrawlStatus, String> {

    @Override
    public String convertToDatabaseColumn(CrawlStatus attribute) {
        return attribute != null ? attribute.getCode() : null;
    }

    @Override
    public CrawlStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? CrawlStatus.fromCode(dbData) : null;
    }
}
