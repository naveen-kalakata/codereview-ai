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

    // Logger — one per class, always private static final
    // LoggerFactory.getLogger(ThisClass.class) ties log messages to this class name
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

        // RAG — retrieve relevant coding standards based on the submitted code
        String relevantStandards = codingStandardsService.findRelevantStandards(code);

        String userMessage = """
                Review the following code against these team coding standards:

                TEAM CODING STANDARDS:
                %s

                CODE TO REVIEW:
                %s
                """.formatted(relevantStandards, code);

        // .entity(ReviewResponse.class) — THIS IS THE MAGIC
        // Spring AI tells Gemini: "respond in JSON matching this class structure"
        // Then automatically deserializes the JSON into a ReviewResponse object
        ReviewResponse response = chatClient.prompt()
                .user(userMessage)
                .call()
                .entity(ReviewResponse.class);

        log.info("Code review complete. Found {} bugs, {} improvements",
                response.getBugs().size(),
                response.getImprovements().size());

        return response;
    }

    // Review a full PR with context — diff + file contents + PR description
    public ReviewResponse reviewPR(PRContext context) {
        log.info("Starting PR review: '{}'", context.getPrTitle());

        // Build the file contents section
        // Stream API: takes a list, transforms each item, joins them into one string
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

    // STREAMING — returns a Flux (reactive stream) of String tokens
    // Flux is from Project Reactor — think of it as a "lazy list that emits items over time"
    // Instead of waiting for Gemini to finish, we get each word as it's generated
    public Flux<String> reviewCodeStream(String code) {
        if (code == null || code.isBlank()) {
            // Flux.error() — creates a stream that immediately emits an error
            // This is the reactive equivalent of "throw new Exception"
            return Flux.error(new IllegalArgumentException("Code cannot be empty"));
        }

        log.info("Starting STREAMING code review, code length: {} characters", code.length());

        // RAG — same retrieval step as the blocking version
        String relevantStandards = codingStandardsService.findRelevantStandards(code);

        String userMessage = """
                Review the following code against these team coding standards:

                TEAM CODING STANDARDS:
                %s

                CODE TO REVIEW:
                %s
                """.formatted(relevantStandards, code);

        // .stream() instead of .call() — THIS IS THE KEY DIFFERENCE
        // .call()   → blocks, waits for full response, returns one object
        // .stream() → returns immediately, emits tokens as Gemini generates them
        //
        // .content() → extracts just the text content from each chunk
        // Returns Flux<String> — each String is a small piece (token) of the response
        return chatClient.prompt()
                .user(userMessage)
                .stream()
                .content();
    }
}
