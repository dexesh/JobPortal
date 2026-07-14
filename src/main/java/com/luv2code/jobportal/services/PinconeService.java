package com.luv2code.jobportal.services;

import io.pinecone.clients.Index;
import io.pinecone.clients.Pinecone;
import io.pinecone.unsigned_indices_model.QueryResponseWithUnsignedIndices;
import io.pinecone.unsigned_indices_model.ScoredVectorWithUnsignedIndices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PinconeService {

    private static final Logger log = LoggerFactory.getLogger(PinconeService.class);

    @Autowired
    private final Pinecone pinecone;

    @Value("${pinecone.index.name:embeddingindex}")
    private String indexName;

    public PinconeService(Pinecone pinecone) {
        this.pinecone = pinecone;
    }

    public void storeEmbedding(float[] embedding, String jobid) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("Cannot store an empty embedding");
        }
        Index index = pinecone.getIndexConnection(indexName);
        ArrayList<Float> vector = new ArrayList<>();
        for (float e : embedding) {
            vector.add(e);
        }
        index.upsert(jobid, vector);
        log.info("[Pinecone] Indexed jobId={} with dimension={}", jobid, embedding.length);
    }

    public List<Integer> searchJobs(float[] embeddings) {
        if (embeddings == null || embeddings.length == 0) {
            return List.of();
        }
        Index index = pinecone.getIndexConnection(indexName);

        // Convert float[] → List<Float>
        List<Float> vectors = new ArrayList<>();
        for (float e : embeddings) {
            vectors.add(e);
        }

        QueryResponseWithUnsignedIndices request = index.query(5, vectors, null, null, null, null, null, false, false);
        // Extract job IDs — skip any non-numeric IDs gracefully
        List<Integer> jobIds = new ArrayList<>();

        for (ScoredVectorWithUnsignedIndices match : request.getMatchesList()) {
            try {
                int jobId = Integer.parseInt(match.getId());
                jobIds.add(jobId);
                log.info("[Pinecone] Match: jobId={}, score={}", jobId, match.getScore());
            } catch (NumberFormatException e) {
                log.warn("[Pinecone] Skipping non-numeric vector ID: '{}'", match.getId());
            }
        }

        return jobIds;
    }
}
