package com.luv2code.jobportal.services;

import io.pinecone.clients.Index;
import io.pinecone.clients.Pinecone;
import io.pinecone.unsigned_indices_model.QueryResponseWithUnsignedIndices;
import io.pinecone.unsigned_indices_model.ScoredVectorWithUnsignedIndices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PinconeService {
    @Autowired
    private final Pinecone pinecone;

    public PinconeService(Pinecone pinecone) {
        this.pinecone = pinecone;
    }

    @Async
    public void storeEmbedding(float[] embedding, String jobid) {
        // Implement the logic to store the embedding in Pinecone using the API key
        // This is a placeholder implementation, you should replace it with actual API calls
        System.out.println("Storing embedding for job ID: " + jobid);
        Index index = pinecone.getIndexConnection("embeddingindex");
        ArrayList<Float> vector = new ArrayList<>();
        for (float e : embedding) {
            vector.add(e);
        }
        index.upsert(jobid, vector);
        System.out.println("Stored in vector database");
    }

    public List<Integer> searchJobs(float[] embeddings) {
        Index index = pinecone.getIndexConnection("embeddingindex");

        // Convert float[] → List<Float>
        List<Float> vectors = new ArrayList<>();
        for (float e : embeddings) {
            vectors.add(e);
        }

        // Build query request
        QueryResponseWithUnsignedIndices request = index.query(5, vectors, null, null, null, null, null, false, false);
        // Execute query
        ;// Execute query


        // Extract job IDs
        List<Integer> jobIds = new ArrayList<>();

        for (ScoredVectorWithUnsignedIndices match : request.getMatchesList()) {
            System.out.println("Job ID: " + match.getId());
            System.out.println("Score: " + match.getScore());

            jobIds.add(Integer.parseInt(match.getId()));
        }

        return jobIds;
    }
}
