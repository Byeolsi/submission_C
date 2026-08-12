package com.noaats.reviewsystem.generator;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DataGeneratorTest {

    @Test
    void testDataGenerationReproducibility() {
        ScenarioConfig config = ScenarioConfig.defaultTestConfig();
        DataGenerator gen1 = new DataGenerator(config);
        DataGenerator gen2 = new DataGenerator(config);
        
        List<RawDocument> docs1 = gen1.generate();
        List<RawDocument> docs2 = gen2.generate();
        
        // Assert reproducibility
        assertEquals(docs1.size(), docs2.size());
        
        if (!docs1.isEmpty()) {
            assertEquals(docs1.get(0).content(), docs2.get(0).content());
        }
        
        // Ensure at least 30 deals generate 120+ documents
        assertTrue(docs1.size() >= 120);
    }
}
