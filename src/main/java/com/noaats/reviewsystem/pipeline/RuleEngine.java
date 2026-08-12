package com.noaats.reviewsystem.pipeline;

import com.noaats.reviewsystem.domain.InvestmentProposal;
import com.noaats.reviewsystem.domain.ReviewStatus;

public class RuleEngine {
    public InvestmentProposal applyRules(InvestmentProposal proposal) {
        if (proposal.status() == ReviewStatus.REVIEW_REQUIRED || proposal.status() == ReviewStatus.REJECTED) {
            return proposal; 
        }
        
        if (proposal.ltv() == null) {
            return new InvestmentProposal(
                proposal.id(), proposal.assetType(), proposal.requestedAmount(), proposal.ltv(),
                proposal.assetSpecificRisks(), ReviewStatus.REJECTED 
            );
        }
        
        // Rule: LTV must be <= 70.0
        if (proposal.ltv().getValue() > 70.0) {
            return new InvestmentProposal(
                proposal.id(), proposal.assetType(), proposal.requestedAmount(), proposal.ltv(),
                proposal.assetSpecificRisks(), ReviewStatus.REJECTED
            );
        }

        return new InvestmentProposal(
            proposal.id(), proposal.assetType(), proposal.requestedAmount(), proposal.ltv(),
            proposal.assetSpecificRisks(), ReviewStatus.APPROVED
        );
    }
}
