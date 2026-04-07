package com.naveen.codereviewai.dto;

// Separate DTO for PR-based reviews
// Example JSON: {"prUrl": "https://github.com/owner/repo/pull/1"}
public class PRReviewRequest {

    private String prUrl;

    public String getPrUrl() {
        return prUrl;
    }

    public void setPrUrl(String prUrl) {
        this.prUrl = prUrl;
    }
}
