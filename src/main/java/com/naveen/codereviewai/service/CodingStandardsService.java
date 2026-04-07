package com.naveen.codereviewai.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CodingStandardsService {

    private static final Logger log = LoggerFactory.getLogger(CodingStandardsService.class);

    // VectorStore — Spring AI's interface for any vector database
    // Right now it's SimpleVectorStore (in-memory). Later we can swap to
    // PgVector, ChromaDB, Pinecone — just change the dependency. Code stays the same.
    private final VectorStore vectorStore;

    public CodingStandardsService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    // @PostConstruct — runs ONCE after Spring creates this bean and injects dependencies
    // Perfect for initialization: "load the coding standards into the vector store at startup"
    @PostConstruct
    public void loadStandards() {
        log.info("Loading coding standards into vector store...");

        try {
            // ClassPathResource — reads files from src/main/resources
            // This is how Spring loads files from inside the jar
            ClassPathResource resource = new ClassPathResource("standards/java-standards.md");
            String content = resource.getContentAsString(StandardCharsets.UTF_8);

            // Split the file by "## " headers — each section becomes a separate document
            // Why split? So we can retrieve ONLY the relevant rules, not the entire file
            String[] sections = content.split("(?=## )");

            List<Document> documents = new ArrayList<>();
            for (String section : sections) {
                String trimmed = section.trim();
                if (!trimmed.isEmpty()) {
                    // Document — Spring AI's wrapper. Contains the text + optional metadata
                    // When we add it to the vector store, Spring AI:
                    //   1. Sends this text to Gemini's embedding API
                    //   2. Gets back a vector (array of numbers)
                    //   3. Stores the text + vector together
                    documents.add(new Document(trimmed));
                }
            }

            // This is where the magic happens:
            // vectorStore.add() → sends each document to the embedding model →
            // gets vectors back → stores text + vectors in memory
            vectorStore.add(documents);
            log.info("Loaded {} coding standards into vector store", documents.size());

        } catch (IOException e) {
            log.error("Failed to load coding standards file", e);
        }
    }

    // Finds the coding standards most relevant to the given code
    // This is the RETRIEVAL part of RAG
    public String findRelevantStandards(String code) {
        log.info("Searching for relevant coding standards...");

        // SearchRequest — "find me the top 3 documents most similar to this code"
        // Under the hood:
        //   1. Converts the code to a vector (embedding)
        //   2. Compares it to every stored vector using cosine similarity
        //   3. Returns the closest matches
        List<Document> relevantDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(code)
                        .topK(3)  // return top 3 most relevant standards
                        .build()
        );

        String standards = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        log.info("Found {} relevant standards", relevantDocs.size());
        return standards;
    }
}
