package com.noaats.reviewsystem;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.noaats.reviewsystem.domain.InvestmentProposal;
import com.noaats.reviewsystem.generator.DataGenerator;
import com.noaats.reviewsystem.generator.RawDocument;
import com.noaats.reviewsystem.generator.ScenarioConfig;
import com.noaats.reviewsystem.pipeline.DataNormalizer;
import com.noaats.reviewsystem.pipeline.GeminiAIService;
import com.noaats.reviewsystem.pipeline.RuleEngine;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

@SpringBootApplication
public class ReviewSystemApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ReviewSystemApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting Alternative Investment Review System...");
        
        boolean generate = false;
        boolean review = false;
        for (String arg : args) {
            if (arg.equals("--generate")) generate = true;
            if (arg.equals("--review")) review = true;
        }

        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        File rawFile = new File("generated-data/raw_documents.json");

        if (generate) {
            System.out.println("Executing Generation Phase...");
            ScenarioConfig config = ScenarioConfig.defaultTestConfig();
            DataGenerator generator = new DataGenerator(config);
            List<RawDocument> documents = generator.generate();
            
            rawFile.getParentFile().mkdirs();
            mapper.writeValue(rawFile, documents);
            
            System.out.println("Successfully generated " + documents.size() + " documents.");
            System.out.println("Saved to " + rawFile.getAbsolutePath());
        }

        if (review) {
            System.out.println("Executing Review Pipeline...");
            if (!rawFile.exists()) {
                System.out.println("No raw data found. Please run with --generate first.");
                return;
            }
            
            List<RawDocument> documents = mapper.readValue(rawFile, new TypeReference<List<RawDocument>>() {});
            
            DataNormalizer normalizer = new DataNormalizer();
            RuleEngine ruleEngine = new RuleEngine();
            GeminiAIService aiService = new GeminiAIService();
            
            List<InvestmentProposal> initialProposals = normalizer.normalize(documents);
            List<InvestmentProposal> finalProposals = new ArrayList<>();
            
            for (InvestmentProposal proposal : initialProposals) {
                // 1. Initial Rule check
                InvestmentProposal evaluated = ruleEngine.applyRules(proposal);
                
                // 2. AI resolution if needed
                if (evaluated.status() == com.noaats.reviewsystem.domain.ReviewStatus.REVIEW_REQUIRED) {
                    evaluated = aiService.analyzeAndResolve(evaluated);
                    // 3. Re-run rules after AI resolution
                    evaluated = ruleEngine.applyRules(evaluated);
                }
                finalProposals.add(evaluated);
            }
            
            File reportFile = new File("output/review_results.json");
            reportFile.getParentFile().mkdirs();
            mapper.writeValue(reportFile, finalProposals);
            System.out.println("Successfully reviewed " + finalProposals.size() + " proposals.");
            System.out.println("Audit Report saved to " + reportFile.getAbsolutePath());
        }
    }
}
