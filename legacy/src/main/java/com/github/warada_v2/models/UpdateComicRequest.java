package com.github.warada_v2.models;

public record UpdateComicRequest(
        String title,
        String description,
        String author,
        String status
) {
}
