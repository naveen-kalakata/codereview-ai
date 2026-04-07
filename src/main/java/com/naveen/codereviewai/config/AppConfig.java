package com.naveen.codereviewai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// @Configuration — tells Spring: "this class contains bean definitions"
// Spring reads this at startup and calls each @Bean method to create objects it manages
@Configuration
public class AppConfig {

    // @Bean — tells Spring: "call this method, take the returned object, and manage it"
    // Now anywhere in the app, if a class needs a RestClient, Spring injects this one
    // SimpleVectorStore — stores vectors in memory (HashMap)
    // EmbeddingModel is auto-created by the Gemini starter — it converts text to vectors
    // We just tell Spring: "use this embedding model to create a simple vector store"
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .build();
    }
}
