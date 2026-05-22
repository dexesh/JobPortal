package com.luv2code.jobportal.services;

import com.luv2code.jobportal.entity.JobPostActivity;
import com.luv2code.jobportal.repository.JobPostActivityRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class JobRecommendationService {
    @Autowired
    private CreateEmbeddingService createEmbeddingService;

    @Autowired
    private PinconeService pinconeService;

    @Autowired
    private JobPostActivityRepository jobPostActivityRepository;

    @Autowired
    private ResumeSummaryService resumeSummaryService;

    @Async
    public CompletableFuture<List<JobPostActivity>> getRecommendedJobs(String rawText) {

        // if (filePath == null || filePath.isEmpty()) {
        // System.out.println("[Recommendation] No file path provided.");
        // return CompletableFuture.completedFuture(Collections.emptyList());
        // }

        // // Convert relative path to absolute
        // File file = new File(filePath);

        // if (!file.isAbsolute()) {
        // file = new File(System.getProperty("user.dir"), filePath);
        // }

        // System.out.println("[Recommendation] Loading PDF from: " +
        // file.getAbsolutePath());

        // if (!file.exists()) {
        // System.out.println("[Recommendation] File NOT found – skipping.");
        // return CompletableFuture.completedFuture(Collections.emptyList());
        // }

        // Step 1: Extract raw text from PDF
        // String rawText;

        // try (PDDocument document = PDDocument.load(file)) {

        // PDFTextStripper pdfStripper = new PDFTextStripper();

        // rawText = pdfStripper.getText(document);

        // System.out.println("[Recommendation] PDF parsed, chars=" + rawText.length());

        // } catch (IOException e) {

        // System.out.println("[Recommendation] Failed to parse PDF: " +
        // e.getMessage());

        // return CompletableFuture.completedFuture(Collections.emptyList());
        // }

        // Step 2: Summarise resume
        String candidateProfile = resumeSummaryService.summarise(rawText);

        if (candidateProfile.isBlank()) {

            System.out.println("[Recommendation] LLM summary was blank – skipping.");

            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        // Step 3: Create embedding
        float[] emb = createEmbeddingService.createEmbeddingOfResume(candidateProfile);

        // Step 4: Search Pinecone
        List<Integer> jobIds = pinconeService.searchJobs(emb);

        System.out.println("[Recommendation] Pinecone returned jobIds: " + jobIds);

        List<JobPostActivity> jobs = jobPostActivityRepository.findJobsByIds(jobIds);

        return CompletableFuture.completedFuture(jobs);
    }
}
