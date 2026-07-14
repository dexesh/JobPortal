package com.luv2code.jobportal.services;

public record CachedCandidateEmbedding(
        int candidateId,
        String resumeHash,
        String model,
        float[] embedding) {
}
