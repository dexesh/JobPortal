package com.luv2code.jobportal.services;

import com.luv2code.jobportal.config.CacheNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class RecommendationSearchCacheService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationSearchCacheService.class);

    private final PinconeService pinconeService;

    public RecommendationSearchCacheService(PinconeService pinconeService) {
        this.pinconeService = pinconeService;
    }

    @Cacheable(
            cacheNames = CacheNames.JOB_RECOMMENDATIONS,
            key = "#candidateId + ':' + #resumeHash + ':' + #jobIndexVersion",
            sync = true)
    public CachedRecommendationIds search(
            int candidateId,
            String resumeHash,
            long jobIndexVersion,
            float[] candidateEmbedding) {
        log.info("[RecommendationCache] Cache miss; querying Pinecone for candidateId={}, jobIndexVersion={}",
                candidateId, jobIndexVersion);
        return new CachedRecommendationIds(pinconeService.searchJobs(candidateEmbedding));
    }
}
