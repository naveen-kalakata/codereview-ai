package com.naveen.codereviewai.learning;

import com.naveen.codereviewai.controller.CodeReviewController;
import com.naveen.codereviewai.dto.ReviewResponse;
import com.naveen.codereviewai.service.CodeReviewService;
import com.naveen.codereviewai.service.GitHubService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ============================================================
// @WebMvcTest vs @SpringBootTest — WHEN TO USE WHICH
// ============================================================
//
// @SpringBootTest
//   → Loads EVERYTHING: controllers, services, databases, Gemini config
//   → Slow (5-10 seconds)
//   → Use for integration tests ("does the whole app work together?")
//
// @WebMvcTest(OneController.class)
//   → Loads ONLY that controller + Spring MVC stuff (Jackson, filters, etc.)
//   → Does NOT load services, databases, Gemini
//   → Fast (~1 second)
//   → Use for controller tests ("does this endpoint return the right HTTP response?")
//
// Since we don't load services, we need @MockitoBean to create fake ones

@WebMvcTest(CodeReviewController.class)
class SpringTestBasicsTest {

    // MockMvc — simulates HTTP requests WITHOUT starting a real Tomcat server
    // It's like Postman, but in code
    @Autowired
    private MockMvc mockMvc;

    // @MockitoBean — like @Mock BUT registers the mock in Spring's context
    // When Spring creates CodeReviewController, it needs CodeReviewService
    // This mock gets injected as if it were the real service
    //
    // @Mock = plain Mockito, works without Spring
    // @MockitoBean = Mockito + Spring DI integration
    @MockitoBean
    private CodeReviewService codeReviewService;

    @MockitoBean
    private GitHubService gitHubService;

    // ============================================================
    // MockMvc basics — simulating HTTP calls
    // ============================================================

    @Test
    void understandingMockMvc() throws Exception {
        // Set up what the mock service returns
        ReviewResponse fake = new ReviewResponse();
        fake.setBugs(List.of("Bug 1", "Bug 2"));
        fake.setImprovements(List.of("Improvement 1"));
        fake.setSecurity(List.of("None found"));
        fake.setTests(List.of("Test 1"));

        when(codeReviewService.reviewCode(anyString())).thenReturn(fake);

        // mockMvc.perform() — "pretend someone sent this HTTP request"
        mockMvc.perform(
                        post("/review")                              // POST method to /review
                                .contentType(MediaType.APPLICATION_JSON)     // Content-Type: application/json
                                .content("{\"code\": \"int x = 1;\"}")       // request body
                )
                // .andExpect() — "I expect the response to have these properties"
                .andExpect(status().isOk())                          // HTTP 200
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // response is JSON

                // jsonPath — reads specific fields from the JSON response
                // $ = root object
                // $.bugs = the "bugs" field
                // $.bugs[0] = first element of bugs array
                // $.bugs.length() = size of bugs array
                .andExpect(jsonPath("$.bugs.length()").value(2))
                .andExpect(jsonPath("$.bugs[0]").value("Bug 1"))
                .andExpect(jsonPath("$.bugs[1]").value("Bug 2"))
                .andExpect(jsonPath("$.improvements[0]").value("Improvement 1"));
    }

    @Test
    void understandingStatusCodes() throws Exception {
        // When service throws IllegalArgumentException, our GlobalExceptionHandler
        // catches it and returns 400 Bad Request
        when(codeReviewService.reviewCode(anyString()))
                .thenThrow(new IllegalArgumentException("Code cannot be empty"));

        mockMvc.perform(
                        post("/review")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\": \"\"}")
                )
                .andExpect(status().isBadRequest());    // HTTP 400

        // Other common status checks:
        // status().isOk()           → 200
        // status().isCreated()      → 201
        // status().isBadRequest()   → 400
        // status().isNotFound()     → 404
        // status().isInternalServerError() → 500
    }

    @Test
    void understandingContentTypes() throws Exception {
        ReviewResponse fake = new ReviewResponse();
        fake.setBugs(List.of());
        fake.setImprovements(List.of());
        fake.setSecurity(List.of());
        fake.setTests(List.of());

        when(codeReviewService.reviewCode(anyString())).thenReturn(fake);

        // Without Content-Type header → Spring doesn't know the body is JSON
        // Our controller expects @RequestBody (JSON), so this would fail
        mockMvc.perform(
                        post("/review")
                                // NO contentType set
                                .content("{\"code\": \"int x = 1;\"}")
                )
                .andExpect(status().isUnsupportedMediaType());  // 415 — "I don't know how to read this"
    }
}
