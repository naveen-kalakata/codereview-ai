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

    private final VectorStore vectorStore;

    public CodingStandardsService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void loadStandards() {
        List<Document> existing = vectorStore.similaritySearch(
                SearchRequest.builder().query("java coding standards").topK(1).build()
        );

        if (!existing.isEmpty()) {
            log.info("Coding standards already loaded in pgvector — skipping reload");
            return;
        }

        log.info("Loading coding standards into pgvector...");

        try {
            ClassPathResource resource = new ClassPathResource("standards/java-standards.md");
            String content = resource.getContentAsString(StandardCharsets.UTF_8);

            String[] sections = content.split("(?=## )");

            List<Document> documents = new ArrayList<>();
            for (String section : sections) {
                String trimmed = section.trim();
                if (!trimmed.isEmpty()) {
                    documents.add(new Document(trimmed));
                }
            }

            vectorStore.add(documents);
            log.info("Loaded {} coding standards into pgvector", documents.size());

        } catch (IOException e) {
            log.error("Failed to load coding standards file", e);
        }
    }

    public String findRelevantStandards(String code) {
        log.info("Searching for relevant coding standards...");

        List<Document> relevantDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(code)
                        .topK(3)
                        .build()
        );

        String standards = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        log.info("Found {} relevant standards", relevantDocs.size());
        return standards;
    }
}
