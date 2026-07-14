package com.luv2code.jobportal.services;

import com.luv2code.jobportal.entity.JobPostActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
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
        String combinedText = String.join(" ",
                value(job.getJobTitle()),
                value(job.getDescriptionOfJob()),
                value(job.getJobType()),
                value(job.getRemote()),
                job.getJobLocationId() == null ? "" : String.join(" ",
                        value(job.getJobLocationId().getCity()),
                        value(job.getJobLocationId().getState()),
                        value(job.getJobLocationId().getCountry())),
                job.getJobCompanyId() == null ? "" : value(job.getJobCompanyId().getName()));
        List<String> chunks = splitText(combinedText, 512);
        if (chunks.isEmpty()) return;

        // Get first chunk to determine embedding dimension
        float[] first = ollama_embedding_service.getEmbedding(chunks.get(0));
        if (first == null || first.length == 0) return;

        float[] sum = Arrays.copyOf(first, first.length);

        // Accumulate remaining chunks
        int successfulChunks = 1;
        for (int i = 1; i < chunks.size(); i++) {
            float[] next = ollama_embedding_service.getEmbedding(chunks.get(i));
            if (next != null && next.length == sum.length) {
                addTo(sum, next);
                successfulChunks++;
            }
        }

        // Mean pool: divide each dimension by number of chunks
        for (int i = 0; i < sum.length; i++) {
            sum[i] /= successfulChunks;
        }

        // Store ONE pooled embedding per job (avoids overwriting same ID each chunk)
        pinconeService.storeEmbedding(sum, String.valueOf(job.getJobPostId()));
    }

    public float[] createEmbeddingOfResume(String resume) {
        List<String> chunks = splitText(resume, 1000); // wider chunks = better context
        if (chunks.isEmpty())
            return new float[0];

        // Get first chunk to know the dimension
        float[] first = ollama_embedding_service.getEmbedding(chunks.get(0));
        if (first == null || first.length == 0) return new float[0];
        int dim = first.length;
        float[] sum = new float[dim];

        // Accumulate all chunk embeddings
        addTo(sum, first);
        int successfulChunks = 1;
        for (int i = 1; i < chunks.size(); i++) {
            float[] next = ollama_embedding_service.getEmbedding(chunks.get(i));
            if (next != null && next.length == dim) {
                addTo(sum, next);
                successfulChunks++;
            }
        }

        // Mean pool: divide each dimension by number of chunks
        for (int i = 0; i < dim; i++) {
            sum[i] /= successfulChunks;
        }
        return sum;
    }

    /** Adds src into dest element-wise (in-place). Guards against mismatched dimensions. */
    private void addTo(float[] dest, float[] src) {
        int len = Math.min(dest.length, src.length);
        for (int i = 0; i < len; i++) {
            dest[i] += src[i];
        }
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
