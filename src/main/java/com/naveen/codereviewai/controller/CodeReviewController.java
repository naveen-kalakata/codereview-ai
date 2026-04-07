package com.naveen.codereviewai.controller;

import com.naveen.codereviewai.dto.PRContext;
import com.naveen.codereviewai.dto.PRReviewRequest;
import com.naveen.codereviewai.dto.ReviewRequest;
import com.naveen.codereviewai.dto.ReviewResponse;
import com.naveen.codereviewai.service.CodeReviewService;
import com.naveen.codereviewai.service.GitHubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

@RestController
public class CodeReviewController {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewController.class);

    private final CodeReviewService codeReviewService;
    private final GitHubService gitHubService;

    public CodeReviewController(CodeReviewService codeReviewService, GitHubService gitHubService) {
        this.codeReviewService = codeReviewService;
        this.gitHubService = gitHubService;
    }

    // Existing endpoint — paste code directly
    @PostMapping("/review")
    public ReviewResponse reviewCode(@RequestBody ReviewRequest request) {
        log.info("Received review request");
        ReviewResponse response = codeReviewService.reviewCode(request.getCode());
        log.info("Review request completed successfully");
        return response;
    }

    // STREAMING endpoint — sends tokens as they're generated via Server-Sent Events (SSE)
    // produces = TEXT_EVENT_STREAM_VALUE tells Spring: "this endpoint streams SSE, not JSON"
    //
    // SSE (Server-Sent Events) — a protocol where the server keeps the HTTP connection open
    // and pushes data to the client line by line. Each line looks like:
    //   data: Here is a
    //   data: piece of the
    //   data: review...
    //
    // The browser/client reads these as they arrive — no waiting for the full response
    @PostMapping(value = "/review/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> reviewCodeStream(@RequestBody ReviewRequest request) {
        log.info("Received STREAMING review request");
        return codeReviewService.reviewCodeStream(request.getCode());
    }

    // New endpoint — review a GitHub PR by URL
    @PostMapping("/review/pr")
    public ReviewResponse reviewPullRequest(@RequestBody PRReviewRequest request) {
        log.info("Received PR review request: {}", request.getPrUrl());

        // Parse the PR URL: https://github.com/owner/repo/pull/123
        String[] parts = parsePrUrl(request.getPrUrl());
        String owner = parts[0];
        String repo = parts[1];
        int prNumber = Integer.parseInt(parts[2]);

        // Fetch full PR context: diff + file contents + PR description
        PRContext context = gitHubService.getPRContext(owner, repo, prNumber);

        // Send everything to Gemini for review
        ReviewResponse response = codeReviewService.reviewPR(context);
        log.info("PR review completed successfully");
        return response;
    }

    // Parses "https://github.com/owner/repo/pull/123" into ["owner", "repo", "123"]
    private String[] parsePrUrl(String prUrl) {
        if (prUrl == null || !prUrl.contains("github.com")) {
            throw new IllegalArgumentException("Invalid GitHub PR URL: " + prUrl);
        }

        // Remove trailing slash if present, then split
        String cleaned = prUrl.replaceAll("/$", "");
        String[] segments = cleaned.split("/");

        // URL: https://github.com/owner/repo/pull/123
        // segments: [https:, , github.com, owner, repo, pull, 123]
        // indices:    0      1      2         3      4     5    6
        if (segments.length < 7 || !"pull".equals(segments[5])) {
            throw new IllegalArgumentException("Invalid GitHub PR URL format. Expected: https://github.com/owner/repo/pull/123");
        }

        return new String[]{segments[3], segments[4], segments[6]};
    }
}
