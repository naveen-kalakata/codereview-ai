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

// @ExtendWith(MockitoExtension.class) — tells JUnit: "initialize @Mock fields before each test"
// This is a UNIT test — no Spring context, no Gemini calls, just pure Java + mocks
@ExtendWith(MockitoExtension.class)
class CodeReviewServiceTest {

    // @Mock — creates a fake version of this object
    // When the test calls methods on it, nothing really happens (returns null by default)
    // We control what it returns using when().thenReturn()
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

    // @BeforeEach — runs before EVERY test method
    // Sets up the mock chain: builder.build() → chatClient, chatClient.prompt() → requestSpec, etc.
    @BeforeEach
    void setUp() {
        // Mock the ChatClient.Builder chain
        when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        codeReviewService = new CodeReviewService(chatClientBuilder, codingStandardsService);
    }

    @Test
    void reviewCode_withValidCode_returnsReview() {
        // ARRANGE — set up what the mocks should return
        String code = "public class Hello { }";

        // Mock RAG — return some fake standards
        when(codingStandardsService.findRelevantStandards(anyString()))
                .thenReturn("Use constructor injection");

        // Build a fake ReviewResponse — this is what Gemini would return
        ReviewResponse fakeResponse = new ReviewResponse();
        fakeResponse.setBugs(List.of("No bugs found"));
        fakeResponse.setImprovements(List.of("Add documentation"));
        fakeResponse.setSecurity(List.of("None found"));
        fakeResponse.setTests(List.of("Test the constructor"));

        // Mock the ChatClient chain: prompt() → user() → call() → entity()
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(ReviewResponse.class)).thenReturn(fakeResponse);

        // ACT — call the method we're testing
        ReviewResponse result = codeReviewService.reviewCode(code);

        // ASSERT — verify the result is what we expect
        assertNotNull(result);
        assertEquals(1, result.getBugs().size());
        assertEquals("No bugs found", result.getBugs().get(0));
        assertEquals("Add documentation", result.getImprovements().get(0));

        // VERIFY — confirm that codingStandardsService was actually called (RAG happened)
        verify(codingStandardsService).findRelevantStandards(anyString());
    }

    @Test
    void reviewCode_withNullCode_throwsException() {
        // No mocking needed — it should throw before reaching ChatClient
        assertThrows(IllegalArgumentException.class, () -> {
            codeReviewService.reviewCode(null);
        });
    }

    @Test
    void reviewCode_withBlankCode_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            codeReviewService.reviewCode("   ");
        });
    }

    @Test
    void reviewCode_withEmptyCode_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            codeReviewService.reviewCode("");
        });
    }
}
