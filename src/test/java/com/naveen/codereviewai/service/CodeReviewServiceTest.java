package com.naveen.codereviewai.service;

import com.naveen.codereviewai.dto.ReviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodeReviewServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private CodingStandardsService codingStandardsService;

    private CodeReviewService codeReviewService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        codeReviewService = new CodeReviewService(chatClientBuilder, codingStandardsService);
    }

    @Test
    void reviewCode_withValidCode_returnsReview() {
        String code = "public class Hello { }";

        when(codingStandardsService.findRelevantStandards(anyString()))
                .thenReturn("Use constructor injection");

        ReviewResponse fakeResponse = new ReviewResponse();
        fakeResponse.setBugs(List.of("No bugs found"));
        fakeResponse.setImprovements(List.of("Add documentation"));
        fakeResponse.setSecurity(List.of("None found"));
        fakeResponse.setTests(List.of("Test the constructor"));

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(ReviewResponse.class)).thenReturn(fakeResponse);

        ReviewResponse result = codeReviewService.reviewCode(code);

        assertNotNull(result);
        assertEquals(1, result.getBugs().size());
        assertEquals("No bugs found", result.getBugs().get(0));
        assertEquals("Add documentation", result.getImprovements().get(0));
        verify(codingStandardsService).findRelevantStandards(anyString());
    }

    @Test
    void reviewCode_withNullCode_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> codeReviewService.reviewCode(null));
    }

    @Test
    void reviewCode_withBlankCode_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> codeReviewService.reviewCode("   "));
    }

    @Test
    void reviewCode_withEmptyCode_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> codeReviewService.reviewCode(""));
    }
}
