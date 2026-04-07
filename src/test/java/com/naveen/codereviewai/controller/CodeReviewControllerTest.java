package com.naveen.codereviewai.controller;

import com.naveen.codereviewai.dto.PRContext;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CodeReviewController.class)
class CodeReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CodeReviewService codeReviewService;

    @MockitoBean
    private GitHubService gitHubService;

    @Test
    void reviewCode_returnsJsonResponse() throws Exception {
        ReviewResponse fakeResponse = new ReviewResponse();
        fakeResponse.setBugs(List.of("Potential null pointer on line 5"));
        fakeResponse.setImprovements(List.of("Use StringBuilder instead of concatenation"));
        fakeResponse.setSecurity(List.of("None found"));
        fakeResponse.setTests(List.of("Test edge case with empty input"));

        when(codeReviewService.reviewCode(anyString())).thenReturn(fakeResponse);

        mockMvc.perform(post("/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"public class Test {}\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bugs[0]").value("Potential null pointer on line 5"))
                .andExpect(jsonPath("$.improvements[0]").value("Use StringBuilder instead of concatenation"))
                .andExpect(jsonPath("$.security[0]").value("None found"))
                .andExpect(jsonPath("$.tests[0]").value("Test edge case with empty input"));
    }

    @Test
    void reviewCode_withEmptyBody_returnsBadRequest() throws Exception {
        when(codeReviewService.reviewCode(anyString()))
                .thenThrow(new IllegalArgumentException("Code cannot be empty"));

        mockMvc.perform(post("/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reviewPR_withInvalidUrl_returnsBadRequest() throws Exception {
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

        when(gitHubService.getPRContext(anyString(), anyString(), anyInt()))
                .thenReturn(new PRContext("fake diff", "Fix bug", "Fixed a null pointer", List.of()));
        when(codeReviewService.reviewPR(any()))
                .thenReturn(fakeResponse);

        mockMvc.perform(post("/review/pr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prUrl\": \"https://github.com/owner/repo/pull/123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bugs[0]").value("Missing null check"));
    }
}
