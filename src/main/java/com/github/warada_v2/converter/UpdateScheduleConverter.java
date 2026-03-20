package com.github.warada_v2.converter;

import com.github.warada_v2.commons.enums.UpdateSchedule;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UpdateScheduleConverter implements AttributeConverter<UpdateSchedule, String> {

    @Override
    public String convertToDatabaseColumn(UpdateSchedule attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public UpdateSchedule convertToEntityAttribute(String dbData) {
        return dbData != null ? UpdateSchedule.fromValue(dbData) : null;
    }
}
