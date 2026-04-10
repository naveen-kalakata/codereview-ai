package com.naveen.codereviewai.dto;

import java.util.List;

public class PRContext {

    private String diff;
    private String prTitle;
    private String prDescription;
    private List<FileContent> files;

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
