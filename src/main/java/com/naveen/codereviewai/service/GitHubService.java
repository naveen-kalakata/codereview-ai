package com.naveen.codereviewai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.naveen.codereviewai.dto.PRContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);

    private final RestClient restClient;

    public GitHubService(RestClient restClient) {
        this.restClient = restClient;
    }

    public PRContext getPRContext(String owner, String repo, int prNumber) {
        log.info("Fetching full PR context: {}/{}/pull/{}", owner, repo, prNumber);

        String diff = fetchDiff(owner, repo, prNumber);
        JsonNode prDetails = fetchPRDetails(owner, repo, prNumber);
        List<PRContext.FileContent> files = fetchChangedFiles(owner, repo, prNumber);

        String title = prDetails.get("title").asText();
        String description = prDetails.has("body") && !prDetails.get("body").isNull()
                ? prDetails.get("body").asText()
                : "No description provided";

        log.info("PR context fetched. Title: '{}', {} files changed", title, files.size());
        return new PRContext(diff, title, description, files);
    }

    private String fetchDiff(String owner, String repo, int prNumber) {
        return restClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{prNumber}", owner, repo, prNumber)
                .header("Accept", "application/vnd.github.v3.diff")
                .retrieve()
                .body(String.class);
    }

    private JsonNode fetchPRDetails(String owner, String repo, int prNumber) {
        return restClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{prNumber}", owner, repo, prNumber)
                .retrieve()
                .body(JsonNode.class);
    }

    private List<PRContext.FileContent> fetchChangedFiles(String owner, String repo, int prNumber) {
        JsonNode filesArray = restClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{prNumber}/files", owner, repo, prNumber)
                .retrieve()
                .body(JsonNode.class);

        List<PRContext.FileContent> files = new ArrayList<>();

        if (filesArray != null && filesArray.isArray()) {
            for (JsonNode file : filesArray) {
                String filename = file.get("filename").asText();
                String status = file.get("status").asText();

                if ("removed".equals(status) || isNonCodeFile(filename)) {
                    continue;
                }

                String rawUrl = file.has("raw_url") ? file.get("raw_url").asText() : null;
                if (rawUrl != null) {
                    try {
                        String content = RestClient.create().get()
                                .uri(rawUrl)
                                .retrieve()
                                .body(String.class);
                        files.add(new PRContext.FileContent(filename, content));
                        log.debug("Fetched content for: {}", filename);
                    } catch (Exception e) {
                        log.warn("Could not fetch content for file: {}", filename);
                    }
                }
            }
        }

        return files;
    }

    private boolean isNonCodeFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".gif")
                || lower.endsWith(".ico") || lower.endsWith(".svg")
                || lower.endsWith(".jar") || lower.endsWith(".pdf")
                || lower.contains("package-lock") || lower.contains("yarn.lock")
                || lower.endsWith(".min.js") || lower.endsWith(".min.css");
    }
}
