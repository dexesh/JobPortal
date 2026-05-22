package com.luv2code.jobportal.services;

import com.luv2code.jobportal.entity.JobPostActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CreateEmbeddingService {
    @Autowired
    private Ollama_Embedding_Service ollama_embedding_service;
    @Autowired
    private PinconeService pinconeService;

    public List<String> splitText(String text, int size) {
        List<String> chunks = new ArrayList<>();

        for (int i = 0; i < text.length(); i += size) {
            chunks.add(text.substring(i, Math.min(text.length(), i + size)));
        }

        return chunks;
    }

    public void createandStoreEmbedding(JobPostActivity job) {
        String description = job.getDescriptionOfJob();
        String title = job.getJobTitle();
        String combinedText = title + " " + description;
        List<String> list = splitText(combinedText, 512);
        float[] embedding = new float[512];
        for (String s : list) {
            embedding = ollama_embedding_service.getEmbedding(s);
            pinconeService.storeEmbedding(embedding, String.valueOf(job.getJobPostId()));
        }
    }

    public float[] createEmbeddingOfResume(String resume) {
        List<String> chunks = splitText(resume, 1000); // wider chunks = better context
        if (chunks.isEmpty())
            return new float[0];

        // Get first chunk to know the dimension
        float[] first = ollama_embedding_service.getEmbedding(chunks.get(0));
        int dim = first.length;
        float[] sum = new float[dim];

        // Accumulate all chunk embeddings
        addTo(sum, first);
        for (int i = 1; i < chunks.size(); i++) {
            addTo(sum, ollama_embedding_service.getEmbedding(chunks.get(i)));
        }

        // Mean pool: divide each dimension by number of chunks
        for (int i = 0; i < dim; i++) {
            sum[i] /= chunks.size();
        }
        return sum;
    }

    /** Adds src into dest element-wise (in-place). */
    private void addTo(float[] dest, float[] src) {
        for (int i = 0; i < dest.length; i++) {
            dest[i] += src[i];
        }
    }
}
