package com.luv2code.jobportal.services;

import java.util.List;

public record CachedRecommendationIds(List<Integer> jobIds) {
    public CachedRecommendationIds {
        jobIds = List.copyOf(jobIds);
    }
}
