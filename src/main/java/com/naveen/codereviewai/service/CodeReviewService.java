package com.naveen.codereviewai.service;

import com.naveen.codereviewai.dto.PRContext;
import com.naveen.codereviewai.dto.ReviewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

import reactor.core.publisher.Flux;

@Service
public class CodeReviewService {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewService.class);

    private final ChatClient chatClient;
    private final CodingStandardsService codingStandardsService;

    private static final String SYSTEM_PROMPT = """
            You are a senior Java developer with 10+ years of experience performing code reviews.
            You review code thoroughly and provide feedback in exactly 4 sections:

            1. bugs: Actual bugs or potential runtime issues
            2. improvements: Code quality, readability, and performance improvements
            3. security: Security vulnerabilities or concerns (write "None found" if none)
            4. tests: Specific areas and scenarios that should be unit tested

            Be concise. Be specific. Reference exact line numbers or variable names when possible.
            """;

    public CodeReviewService(ChatClient.Builder chatClientBuilder,
                             CodingStandardsService codingStandardsService) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
        this.codingStandardsService = codingStandardsService;
        log.info("CodeReviewService initialized with system prompt");
    }

    public ReviewResponse reviewCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Code cannot be empty");
        }

        log.info("Starting code review, code length: {} characters", code.length());

        String relevantStandards = codingStandardsService.findRelevantStandards(code);

        String userMessage = """
                Review the following code against these team coding standards:

                TEAM CODING STANDARDS:
                %s

                CODE TO REVIEW:
                %s
                """.formatted(relevantStandards, code);

        ReviewResponse response = chatClient.prompt()
                .user(userMessage)
                .call()
                .entity(ReviewResponse.class);

        log.info("Code review complete. Found {} bugs, {} improvements",
                response.getBugs().size(),
                response.getImprovements().size());

        return response;
    }

    public ReviewResponse reviewPR(PRContext context) {
        log.info("Starting PR review: '{}'", context.getPrTitle());

        String fileContents = context.getFiles().stream()
                .map(file -> "=== FILE: %s ===\n%s".formatted(file.getFilename(), file.getContent()))
                .collect(Collectors.joining("\n\n"));

        String userMessage = """
                Review this Pull Request.

                PR TITLE: %s

                PR DESCRIPTION: %s

                DIFF (what changed):
                %s

                FULL FILE CONTENTS (for context):
                %s
                """.formatted(
                context.getPrTitle(),
                context.getPrDescription(),
                context.getDiff(),
                fileContents
        );

        ReviewResponse response = chatClient.prompt()
                .user(userMessage)
                .call()
                .entity(ReviewResponse.class);

        log.info("PR review complete. Found {} bugs, {} improvements",
                response.getBugs().size(),
                response.getImprovements().size());

        return response;
    }

    public Flux<String> reviewCodeStream(String code) {
        if (code == null || code.isBlank()) {
            return Flux.error(new IllegalArgumentException("Code cannot be empty"));
        }

        log.info("Starting streaming code review, code length: {} characters", code.length());

        String relevantStandards = codingStandardsService.findRelevantStandards(code);

        String userMessage = """
                Review the following code against these team coding standards:

                TEAM CODING STANDARDS:
                %s

                CODE TO REVIEW:
                %s
                """.formatted(relevantStandards, code);

        return chatClient.prompt()
                .user(userMessage)
                .stream()
                .content();
    }
}
