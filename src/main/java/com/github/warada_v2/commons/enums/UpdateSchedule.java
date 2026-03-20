package com.github.warada_v2.commons.enums;

import lombok.Getter;

@Getter
public enum UpdateSchedule {
    WEEK("WEEK", "Update on a specific day of the week"),
    MONTH("MONTH", "Update on a specific day of the month"),
    NONE("NONE", "No automatic update scheduled");

    private final String value;
    private final String description;

    UpdateSchedule(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static UpdateSchedule fromValue(String value) {
        for (UpdateSchedule schedule : UpdateSchedule.values()) {
            if (schedule.value.equalsIgnoreCase(value)) {
                return schedule;
            }
        }
        throw new IllegalArgumentException("Unknown UpdateSchedule value: " + value);
    }
}
