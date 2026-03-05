package com.github.warada_v2.commons.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.Objects;

@Getter
public enum ImageType {
    THUMB_IMAGE("THUMB", "Thumb"),
    CHAPTER_IMAGE("CHAPTER_IMAGE", "Chapter");

    private final String value;
    private final String description;

    ImageType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @JsonCreator
    public static ImageType fromValue(String value) {
        for (ImageType type : ImageType.values()) {
            if (Objects.equals(type.value, value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ImageType value: " + value);
    }
}
