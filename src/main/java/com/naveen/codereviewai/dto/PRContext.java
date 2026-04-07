package com.naveen.codereviewai.dto;

import java.util.List;

// Holds everything the AI needs to review a PR intelligently
public class PRContext {

    private String diff;                // what changed
    private String prTitle;             // what the developer says they did
    private String prDescription;       // why they did it
    private List<FileContent> files;    // full content of changed files

    // Inner class — a simple container for filename + content
    // Lives inside PRContext because it only makes sense in this context
    public static class FileContent {
        private String filename;
        private String content;

        public FileContent(String filename, String content) {
            this.filename = filename;
            this.content = content;
        }

        public String getFilename() {
            return filename;
        }

        public String getContent() {
            return content;
        }
    }

    public PRContext(String diff, String prTitle, String prDescription, List<FileContent> files) {
        this.diff = diff;
        this.prTitle = prTitle;
        this.prDescription = prDescription;
        this.files = files;
    }

    public String getDiff() {
        return diff;
    }

    public String getPrTitle() {
        return prTitle;
    }

    public String getPrDescription() {
        return prDescription;
    }

    public List<FileContent> getFiles() {
        return files;
    }
}
