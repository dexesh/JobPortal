package com.luv2code.jobportal.services;

import com.luv2code.jobportal.config.CacheNames;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class CandidateEmbeddingCacheService {

    private static final Logger log = LoggerFactory.getLogger(CandidateEmbeddingCacheService.class);

    private final ResumeSummaryService resumeSummaryService;
    private final CreateEmbeddingService createEmbeddingService;

    @Value("${spring.ai.ollama.embedding.options.model:mxbai-embed-large}")
    private String embeddingModel;

    public CandidateEmbeddingCacheService(
            ResumeSummaryService resumeSummaryService,
            CreateEmbeddingService createEmbeddingService) {
        this.resumeSummaryService = resumeSummaryService;
        this.createEmbeddingService = createEmbeddingService;
    }

    @Cacheable(
            cacheNames = CacheNames.CANDIDATE_EMBEDDINGS,
            key = "#candidateId + ':' + #resumeHash + ':' + #root.target.embeddingModel",
            sync = true)
    public CachedCandidateEmbedding getOrCreate(int candidateId, File resumeFile, String resumeHash) throws IOException {
        log.info("[RecommendationCache] Cache miss; processing resume for candidateId={}", candidateId);

        String rawText;
        try (PDDocument document = PDDocument.load(resumeFile)) {
            rawText = new PDFTextStripper().getText(document);
        }
        if (rawText == null || rawText.isBlank()) {
            throw new IOException("Resume contains no extractable text");
        }

        String candidateProfile = resumeSummaryService.summarise(rawText);
        if (candidateProfile == null || candidateProfile.isBlank()) {
            throw new IllegalStateException("Resume analysis returned no content");
        }

        float[] embedding = createEmbeddingService.createEmbeddingOfResume(candidateProfile);
        if (embedding == null || embedding.length == 0) {
            throw new IllegalStateException("Embedding service returned no vector");
        }

        return new CachedCandidateEmbedding(candidateId, resumeHash, embeddingModel, embedding);
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }
}
