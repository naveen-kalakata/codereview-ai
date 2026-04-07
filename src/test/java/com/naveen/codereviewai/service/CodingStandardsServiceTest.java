package com.naveen.codereviewai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodingStandardsServiceTest {

    @Mock
    private VectorStore vectorStore;

    private CodingStandardsService codingStandardsService;

    @BeforeEach
    void setUp() {
        codingStandardsService = new CodingStandardsService(vectorStore);
    }

    @Test
    void findRelevantStandards_returnsMatchingStandards() {
        // ARRANGE — mock the vector store to return fake documents
        Document doc1 = new Document("Use constructor injection instead of field injection");
        Document doc2 = new Document("Always use SLF4J for logging");

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(doc1, doc2));

        // ACT
        String result = codingStandardsService.findRelevantStandards("@Autowired private Service s;");

        // ASSERT — the result should contain both standards joined together
        assertNotNull(result);
        assertTrue(result.contains("constructor injection"));
        assertTrue(result.contains("SLF4J"));

        // VERIFY — confirm similarity search was actually called
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void findRelevantStandards_withNoMatches_returnsEmpty() {
        // Vector store finds nothing relevant
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of());

        String result = codingStandardsService.findRelevantStandards("some random code");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findRelevantStandards_searchesWithCorrectTopK() {
        // ArgumentCaptor — captures the actual argument passed to a mock method
        // So we can inspect WHAT was passed, not just that it was called
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of());

        codingStandardsService.findRelevantStandards("test code");

        // Capture the SearchRequest that was passed to similaritySearch()
        verify(vectorStore).similaritySearch(captor.capture());

        SearchRequest captured = captor.getValue();
        // Verify we're asking for top 3 results (our RAG config)
        assertEquals(3, captured.getTopK());
    }
}
