package com.noaats.reviewsystem.domain;

public record History(
    String modifiedBy,
    String timestamp,
    String reason,
    Object previousValue
) {}
