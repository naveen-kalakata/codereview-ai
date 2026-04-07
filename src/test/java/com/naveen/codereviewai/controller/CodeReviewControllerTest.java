package com.naveen.codereviewai.controller;

import com.naveen.codereviewai.dto.ReviewResponse;
import com.naveen.codereviewai.service.CodeReviewService;
import com.naveen.codereviewai.service.GitHubService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest — loads ONLY the controller layer. No service beans, no database, no Gemini.
// Much faster than @SpringBootTest because it doesn't load the full application context.
// We mock the services this controller depends on.
@WebMvcTest(CodeReviewController.class)
class CodeReviewControllerTest {

    // MockMvc — Spring's tool for testing HTTP requests without starting a real server
    // It simulates HTTP calls: "pretend someone sent POST /review with this body"
    @Autowired
    private MockMvc mockMvc;

    // @MockitoBean — creates a mock AND registers it in Spring's context
    // Different from @Mock: @Mock is plain Mockito, @MockitoBean works with Spring's DI
    @MockitoBean
    private CodeReviewService codeReviewService;

    @MockitoBean
    private GitHubService gitHubService;

    @Test
    void reviewCode_returnsJsonResponse() throws Exception {
        // ARRANGE — set up what the mock service returns
        ReviewResponse fakeResponse = new ReviewResponse();
        fakeResponse.setBugs(List.of("Potential null pointer on line 5"));
        fakeResponse.setImprovements(List.of("Use StringBuilder instead of concatenation"));
        fakeResponse.setSecurity(List.of("None found"));
        fakeResponse.setTests(List.of("Test edge case with empty input"));

        when(codeReviewService.reviewCode(anyString())).thenReturn(fakeResponse);

        // ACT & ASSERT — send a POST request and verify the response
        mockMvc.perform(post("/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"public class Test {}\"}"))
                // Verify HTTP 200
                .andExpect(status().isOk())
                // Verify response is JSON
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Verify JSON structure — $.bugs[0] means "first element of bugs array"
                .andExpect(jsonPath("$.bugs[0]").value("Potential null pointer on line 5"))
                .andExpect(jsonPath("$.improvements[0]").value("Use StringBuilder instead of concatenation"))
                .andExpect(jsonPath("$.security[0]").value("None found"))
                .andExpect(jsonPath("$.tests[0]").value("Test edge case with empty input"));
    }

    @Test
    void reviewCode_withEmptyBody_returnsBadRequest() throws Exception {
        // Send request with empty code — should trigger IllegalArgumentException
        when(codeReviewService.reviewCode(anyString()))
                .thenThrow(new IllegalArgumentException("Code cannot be empty"));

        mockMvc.perform(post("/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reviewPR_withInvalidUrl_returnsBadRequest() throws Exception {
        // Send a garbage URL — controller's parsePrUrl() should reject it
        mockMvc.perform(post("/review/pr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prUrl\": \"not-a-url\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reviewPR_withValidUrl_returnsReview() throws Exception {
        ReviewResponse fakeResponse = new ReviewResponse();
        fakeResponse.setBugs(List.of("Missing null check"));
        fakeResponse.setImprovements(List.of("Extract method"));
        fakeResponse.setSecurity(List.of("None found"));
        fakeResponse.setTests(List.of("Test PR changes"));

        when(gitHubService.getPRContext(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new com.naveen.codereviewai.dto.PRContext("fake diff", "Fix bug", "Fixed a null pointer", List.of()));
        when(codeReviewService.reviewPR(org.mockito.ArgumentMatchers.any()))
                .thenReturn(fakeResponse);

        mockMvc.perform(post("/review/pr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prUrl\": \"https://github.com/owner/repo/pull/123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bugs[0]").value("Missing null check"));
    }
}
