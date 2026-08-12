package com.noaats.reviewsystem.generator;

public record RawDocument(
    String dealId,
    String docType,
    String content,
    String timestamp
) {}
