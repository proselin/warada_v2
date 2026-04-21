package com.github.warada_v2.converter;

import com.github.warada_v2.commons.enums.ImageType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ImageTypeConverter implements AttributeConverter<ImageType, String> {

    @Override
    public String convertToDatabaseColumn(ImageType attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public ImageType convertToEntityAttribute(String dbData) {
        return dbData != null ? ImageType.fromValue(dbData) : null;
    }
}