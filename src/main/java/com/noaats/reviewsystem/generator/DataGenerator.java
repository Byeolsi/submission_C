package com.noaats.reviewsystem.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.noaats.reviewsystem.domain.AssetType;

public class DataGenerator {
    private final ScenarioConfig config;
    private final Random random;

    public DataGenerator(ScenarioConfig config) {
        this.config = config;
        this.random = new Random(config.seed());
    }

    public List<RawDocument> generate() {
        List<RawDocument> documents = new ArrayList<>();
        AssetType[] assetTypes = AssetType.values();
        
        for (int i = 1; i <= config.numberOfDeals(); i++) {
            String dealId = "DEAL-" + String.format("%03d", i);
            AssetType type = assetTypes[random.nextInt(assetTypes.length)];
            
            double baseLtv = 50.0 + random.nextDouble() * 30.0;
            double requestedAmount = 100.0 + random.nextDouble() * 900.0;
            
            // IM Document
            if (random.nextDouble() >= config.missingFieldProbability()) {
                String amountStr = String.format("%.1f억", requestedAmount);
                if (random.nextDouble() < config.unitErrorProbability()) {
                    amountStr = String.format("%.1f천원", requestedAmount * 100000); // SC-02: Unit Error
                }
                String imContent = String.format("Asset: %s. Requested Amount: %s. LTV is %.1f%%.", type, amountStr, baseLtv);
                documents.add(new RawDocument(dealId, "IM", imContent, "2026-08-01T10:00:00Z"));
            }
            
            // Financial Model Document
            double modelLtv = baseLtv;
            if (random.nextDouble() < config.conflictProbability()) {
                modelLtv += 5.0; // SC-01: Conflict
            }
            
            String fmContent = String.format("Calculated LTV: %.1f%%.", modelLtv);
            documents.add(new RawDocument(dealId, "FinancialModel", fmContent, "2026-08-02T15:30:00Z"));
            
            // Term Sheet Document (SC-12, SC-14)
            String dateStr = random.nextBoolean() ? "2026-08-03" : "08/03/2026"; // SC-12 Format mismatch
            String termContent = String.format("Term Sheet Date: %s. Conditional LTV: If A then %.1f%% else %.1f%%.", dateStr, baseLtv, baseLtv + 10); // SC-14 Conditional
            documents.add(new RawDocument(dealId, "TermSheet", termContent, "2026-08-03T10:00:00Z"));
            
            // Due Diligence Report (SC-13 Structure fail)
            String ddContent = random.nextBoolean() ? "LTV\t" + baseLtv + "%" : "LTV    " + baseLtv + "%";
            documents.add(new RawDocument(dealId, "DueDiligence", ddContent, "2026-08-04T11:00:00Z"));

            // Email Document (SC-05)
            if (random.nextDouble() < 0.15) {
                documents.add(new RawDocument(dealId, "Email", "Update: The requested amount has been adjusted.", "2026-08-05T09:00:00Z"));
            }
            
            // Messenger Document (SC-15 Unofficial Source)
            if (random.nextDouble() < 0.1) {
                documents.add(new RawDocument(dealId, "Messenger", "Hey, LTV is confirmed at " + baseLtv + "%", "2026-08-06T18:00:00Z"));
            }
        }
        
        return documents;
    }
}
