package com.luv2code.jobportal.services;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class Ollama_Embedding_Service {
     private final EmbeddingModel embeddingModel;


    public Ollama_Embedding_Service(EmbeddingModel embeddingModel) {
        this.embeddingModel=embeddingModel;

    }

     public float[] getEmbedding(String text) {
        // Call the Ollama API to get the embedding for the given text
        // This is a placeholder implementation, you should replace it with actual API calls
        return embeddingModel.embed(text);// Assuming the embedding size is 512
    }

}
