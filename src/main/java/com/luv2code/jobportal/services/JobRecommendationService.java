package com.luv2code.jobportal.services;

import com.luv2code.jobportal.entity.JobPostActivity;
import com.luv2code.jobportal.repository.JobPostActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class JobRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(JobRecommendationService.class);

    private final CandidateEmbeddingCacheService candidateEmbeddingCacheService;
    private final ResumeFingerprintService resumeFingerprintService;
    private final RecommendationSearchCacheService recommendationSearchCacheService;
    private final JobIndexVersionService jobIndexVersionService;
    private final JobPostActivityRepository jobPostActivityRepository;

    public JobRecommendationService(
            CandidateEmbeddingCacheService candidateEmbeddingCacheService,
            ResumeFingerprintService resumeFingerprintService,
            RecommendationSearchCacheService recommendationSearchCacheService,
            JobIndexVersionService jobIndexVersionService,
            JobPostActivityRepository jobPostActivityRepository) {
        this.candidateEmbeddingCacheService = candidateEmbeddingCacheService;
        this.resumeFingerprintService = resumeFingerprintService;
        this.recommendationSearchCacheService = recommendationSearchCacheService;
        this.jobIndexVersionService = jobIndexVersionService;
        this.jobPostActivityRepository = jobPostActivityRepository;
    }

    public record RecommendationResult(String status, String message, List<JobPostActivity> jobs) {
        public static RecommendationResult success(List<JobPostActivity> jobs) {
            return new RecommendationResult(jobs.isEmpty() ? "NO_MATCHES" : "OK",
                    jobs.isEmpty() ? "No sufficiently relevant jobs were found." : "", jobs);
        }

        public static RecommendationResult failure(String status, String message) {
            return new RecommendationResult(status, message, Collections.emptyList());
        }
    }

    @Async
    public CompletableFuture<RecommendationResult> getRecommendedJobs(int candidateId, String filePath) {
        try {
            if (filePath == null || filePath.isBlank()) {
                return completedFailure("NO_RESUME", "Upload a resume to get personalised recommendations.");
            }

            File file = Paths.get(filePath).toAbsolutePath().normalize().toFile();
            if (!file.isFile()) {
                return completedFailure("NO_RESUME", "The uploaded resume file could not be found.");
            }

            String resumeHash = resumeFingerprintService.sha256(file);
            CachedCandidateEmbedding cachedEmbedding = candidateEmbeddingCacheService
                    .getOrCreate(candidateId, file, resumeHash);

            float[] embedding = cachedEmbedding.embedding();
            if (embedding == null || embedding.length == 0) {
                return completedFailure("AI_UNAVAILABLE", "The embedding service returned no vector.");
            }

            long jobIndexVersion = jobIndexVersionService.currentVersion();
            List<Integer> jobIds = recommendationSearchCacheService
                    .search(candidateId, resumeHash, jobIndexVersion, embedding)
                    .jobIds();
            log.info("[Recommendation] candidateId={}, resumeHash={}, jobIndexVersion={}, jobIds={}",
                    candidateId, shortHash(resumeHash), jobIndexVersion, jobIds);

            if (jobIds.isEmpty()) {
                return CompletableFuture.completedFuture(RecommendationResult.success(Collections.emptyList()));
            }

            List<JobPostActivity> unorderedJobs = jobPostActivityRepository.findJobsByIds(jobIds);
            Map<Integer, JobPostActivity> jobsById = new HashMap<>();
            unorderedJobs.forEach(job -> jobsById.put(job.getJobPostId(), job));

            List<JobPostActivity> rankedJobs = new ArrayList<>();
            for (Integer jobId : jobIds) {
                JobPostActivity job = jobsById.get(jobId);
                if (job != null) rankedJobs.add(job);
            }
            return CompletableFuture.completedFuture(RecommendationResult.success(rankedJobs));

        } catch (IOException exception) {
            log.error("[Recommendation] Resume could not be processed for candidateId={}", candidateId, exception);
            return completedFailure("INVALID_RESUME", "The resume could not be read as a PDF.");
        } catch (Exception exception) {
            log.error("[Recommendation] Unexpected recommendation failure for candidateId={}", candidateId, exception);
            return completedFailure("SERVICE_UNAVAILABLE",
                    "Recommendations are temporarily unavailable. Please try again later.");
        }
    }

    private CompletableFuture<RecommendationResult> completedFailure(String status, String message) {
        return CompletableFuture.completedFuture(RecommendationResult.failure(status, message));
    }

    private String shortHash(String hash) {
        return hash.length() <= 12 ? hash : hash.substring(0, 12);
    }
}
