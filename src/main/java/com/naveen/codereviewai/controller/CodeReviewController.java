package com.naveen.codereviewai.controller;

import com.naveen.codereviewai.dto.PRReviewRequest;
import com.naveen.codereviewai.dto.ReviewRequest;
import com.naveen.codereviewai.dto.ReviewResponse;
import com.naveen.codereviewai.service.CodeReviewService;
import com.naveen.codereviewai.service.GitHubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @PostMapping("/review")
    public ReviewResponse reviewCode(@RequestBody ReviewRequest request) {
        log.info("Received review request");
        ReviewResponse response = codeReviewService.reviewCode(request.getCode());
        log.info("Review request completed successfully");
        return response;
    }

    @PostMapping(value = "/review/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> reviewCodeStream(@RequestBody ReviewRequest request) {
        log.info("Received streaming review request");
        return codeReviewService.reviewCodeStream(request.getCode());
    }

    @PostMapping("/review/pr")
    public ReviewResponse reviewPullRequest(@RequestBody PRReviewRequest request) {
        log.info("Received PR review request: {}", request.getPrUrl());

        String[] parts = parsePrUrl(request.getPrUrl());
        String owner = parts[0];
        String repo = parts[1];
        int prNumber = Integer.parseInt(parts[2]);

        var context = gitHubService.getPRContext(owner, repo, prNumber);
        ReviewResponse response = codeReviewService.reviewPR(context);
        log.info("PR review completed successfully");
        return response;
    }

    private String[] parsePrUrl(String prUrl) {
        if (prUrl == null || !prUrl.contains("github.com")) {
            throw new IllegalArgumentException("Invalid GitHub PR URL: " + prUrl);
        }

        String cleaned = prUrl.replaceAll("/$", "");
        String[] segments = cleaned.split("/");

        if (segments.length < 7 || !"pull".equals(segments[5])) {
            throw new IllegalArgumentException("Invalid GitHub PR URL format. Expected: https://github.com/owner/repo/pull/123");
        }

        return new String[]{segments[3], segments[4], segments[6]};
    }
}
