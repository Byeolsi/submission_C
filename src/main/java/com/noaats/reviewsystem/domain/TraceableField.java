package com.noaats.reviewsystem.domain;

import java.util.ArrayList;
import java.util.List;
import java.time.Instant;

public class TraceableField<T> {
    private final T value;
    private final double confidence;
    private final String source;
    private final boolean hasConflict;
    private final List<History> lineage;

    public TraceableField(T value, double confidence, String source, boolean hasConflict, List<History> lineage) {
        this.value = value;
        this.confidence = confidence;
        this.source = source;
        this.hasConflict = hasConflict;
        this.lineage = lineage != null ? List.copyOf(lineage) : List.of();
    }

    public static <T> TraceableField<T> of(T value, String source) {
        return new TraceableField<>(value, 1.0, source, false, List.of());
    }

    public TraceableField<T> withValue(T newValue, String modifiedBy, String reason, double newConfidence, boolean conflict) {
        List<History> newLineage = new ArrayList<>(this.lineage);
        newLineage.add(new History(modifiedBy, Instant.now().toString(), reason, this.value));
        return new TraceableField<>(newValue, newConfidence, modifiedBy, conflict, newLineage);
    }

    public T getValue() { return value; }
    public double getConfidence() { return confidence; }
    public String getSource() { return source; }
    public boolean hasConflict() { return hasConflict; }
    public List<History> getLineage() { return lineage; }

    @Override
    public String toString() {
        return "TraceableField{value=" + value + ", source='" + source + "', confidence=" + confidence + ", conflict=" + hasConflict + "}";
    }
}
