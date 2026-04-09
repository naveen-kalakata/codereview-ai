package com.naveen.codereviewai.dto;

import java.util.List;

public class ReviewResponse {

    private List<String> bugs;
    private List<String> improvements;
    private List<String> security;
    private List<String> tests;

    public ReviewResponse() {
    }

    public List<String> getBugs() {
        return bugs;
    }

    public void setBugs(List<String> bugs) {
        this.bugs = bugs;
    }

    public List<String> getImprovements() {
        return improvements;
    }

    public void setImprovements(List<String> improvements) {
        this.improvements = improvements;
    }

    public List<String> getSecurity() {
        return security;
    }

    public void setSecurity(List<String> security) {
        this.security = security;
    }

    public List<String> getTests() {
        return tests;
    }

    public void setTests(List<String> tests) {
        this.tests = tests;
    }
}
