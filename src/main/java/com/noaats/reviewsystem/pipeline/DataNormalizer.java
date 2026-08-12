package com.noaats.reviewsystem.pipeline;

import com.noaats.reviewsystem.domain.*;
import com.noaats.reviewsystem.generator.RawDocument;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataNormalizer {

    private static final Pattern LTV_PATTERN = Pattern.compile("LTV.*?([0-9]+(?:\\.[0-9]+)?)\\s*%");
    private static final Pattern ASSET_PATTERN = Pattern.compile("Asset:\\s*([A-Z_]+)");

    public List<InvestmentProposal> normalize(List<RawDocument> documents) {
        Map<String, List<RawDocument>> grouped = new HashMap<>();
        for (RawDocument doc : documents) {
            grouped.computeIfAbsent(doc.dealId(), k -> new ArrayList<>()).add(doc);
        }

        List<InvestmentProposal> proposals = new ArrayList<>();
        for (Map.Entry<String, List<RawDocument>> entry : grouped.entrySet()) {
            proposals.add(processDeal(entry.getKey(), entry.getValue()));
        }
        return proposals;
    }

    private InvestmentProposal processDeal(String dealId, List<RawDocument> docs) {
        TraceableField<Double> ltvField = null;
        AssetType assetType = AssetType.REAL_ESTATE; 
        
        for (RawDocument doc : docs) {
            if (doc.content().contains("Asset:")) {
                Matcher m = ASSET_PATTERN.matcher(doc.content());
                if (m.find()) {
                    try {
                        assetType = AssetType.valueOf(m.group(1));
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            Matcher ltvMatcher = LTV_PATTERN.matcher(doc.content());
            if (ltvMatcher.find()) {
                double parsedLtv = Double.parseDouble(ltvMatcher.group(1));
                if (ltvField == null) {
                    ltvField = TraceableField.of(parsedLtv, doc.docType() + " (" + doc.timestamp() + ")");
                } else {
                    if (Math.abs(ltvField.getValue() - parsedLtv) > 0.01) {
                        // SC-01 Conflict Detected
                        ltvField = ltvField.withValue(
                            ltvField.getValue(), 
                            "System Normalizer",
                            "Conflict detected with " + doc.docType() + " value: " + parsedLtv,
                            0.5,
                            true
                        );
                    }
                }
            }
        }
        
        ReviewStatus initialStatus = (ltvField != null && ltvField.hasConflict()) ? ReviewStatus.REVIEW_REQUIRED : ReviewStatus.PENDING;
        if (ltvField == null) {
             initialStatus = ReviewStatus.REJECTED; // Missing mandatory field
        }

        return new InvestmentProposal(
            dealId,
            assetType,
            TraceableField.of(0.0, "Placeholder"),
            ltvField,
            new HashMap<>(),
            initialStatus
        );
    }
}
