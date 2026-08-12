package com.noaats.reviewsystem.domain;

import java.util.Map;

public record InvestmentProposal(
    String id,
    AssetType assetType,
    TraceableField<Double> requestedAmount,
    TraceableField<Double> ltv,
    Map<String, TraceableField<Object>> assetSpecificRisks,
    ReviewStatus status
) {}
