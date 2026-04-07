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
        Document doc1 = new Document("Use constructor injection instead of field injection");
        Document doc2 = new Document("Always use SLF4J for logging");

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(doc1, doc2));

        String result = codingStandardsService.findRelevantStandards("@Autowired private Service s;");

        assertNotNull(result);
        assertTrue(result.contains("constructor injection"));
        assertTrue(result.contains("SLF4J"));
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void findRelevantStandards_withNoMatches_returnsEmpty() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of());

        String result = codingStandardsService.findRelevantStandards("some random code");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findRelevantStandards_searchesWithCorrectTopK() {
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of());

        codingStandardsService.findRelevantStandards("test code");

        verify(vectorStore).similaritySearch(captor.capture());
        assertEquals(3, captor.getValue().getTopK());
    }
}
