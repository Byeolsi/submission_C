package com.noaats.reviewsystem.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TraceableFieldTest {

    @Test
    void testTraceableFieldCreation() {
        TraceableField<Double> ltv = TraceableField.of(65.0, "IM Document");
        
        assertEquals(65.0, ltv.getValue());
        assertEquals("IM Document", ltv.getSource());
        assertEquals(1.0, ltv.getConfidence());
        assertFalse(ltv.hasConflict());
        assertTrue(ltv.getLineage().isEmpty());
    }

    @Test
    void testTraceableFieldUpdateWithHistory() {
        TraceableField<Double> initial = TraceableField.of(65.0, "IM Document");
        
        // Simulating an AI or Rule updating the value due to a conflict or new finding
        TraceableField<Double> updated = initial.withValue(
            75.0, 
            "Gemini AI", 
            "Found updated LTV in Appendix B", 
            0.85, 
            true
        );

        assertEquals(75.0, updated.getValue());
        assertEquals("Gemini AI", updated.getSource());
        assertEquals(0.85, updated.getConfidence());
        assertTrue(updated.hasConflict());
        
        assertEquals(1, updated.getLineage().size());
        History history = updated.getLineage().get(0);
        assertEquals("Gemini AI", history.modifiedBy());
        assertEquals(65.0, history.previousValue());
        assertEquals("Found updated LTV in Appendix B", history.reason());
    }
}
