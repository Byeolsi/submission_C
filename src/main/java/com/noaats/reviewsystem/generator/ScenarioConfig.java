package com.noaats.reviewsystem.generator;

public record ScenarioConfig(
    long seed,
    int numberOfDeals,
    double conflictProbability,
    double missingFieldProbability,
    double unitErrorProbability
) {
    public static ScenarioConfig defaultTestConfig() {
        return new ScenarioConfig(12345L, 30, 0.2, 0.1, 0.1);
    }
}
