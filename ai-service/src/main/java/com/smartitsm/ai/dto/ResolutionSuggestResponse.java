package com.smartitsm.ai.dto;

public class ResolutionSuggestResponse {

    private String ticketId;
    private String suggestedResolution;
    private double confidence;
    private boolean autoExecutable;
    private Step[] steps;
    private String[] requiredPermissions;
    private long estimatedEffortMinutes;
    private SimilarCase[] similarCases;
    private java.time.Instant generatedAt;

    public static class Step {
        private int order;
        private String description;
        private String command;
        private String target;

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }
    }

    public static class SimilarCase {
        private String caseId;
        private String title;
        private String resolution;
        private double similarity;

        public String getCaseId() {
            return caseId;
        }

        public void setCaseId(String caseId) {
            this.caseId = caseId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getResolution() {
            return resolution;
        }

        public void setResolution(String resolution) {
            this.resolution = resolution;
        }

        public double getSimilarity() {
            return similarity;
        }

        public void setSimilarity(double similarity) {
            this.similarity = similarity;
        }
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getSuggestedResolution() {
        return suggestedResolution;
    }

    public void setSuggestedResolution(String suggestedResolution) {
        this.suggestedResolution = suggestedResolution;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public boolean isAutoExecutable() {
        return autoExecutable;
    }

    public void setAutoExecutable(boolean autoExecutable) {
        this.autoExecutable = autoExecutable;
    }

    public Step[] getSteps() {
        return steps;
    }

    public void setSteps(Step[] steps) {
        this.steps = steps;
    }

    public String[] getRequiredPermissions() {
        return requiredPermissions;
    }

    public void setRequiredPermissions(String[] requiredPermissions) {
        this.requiredPermissions = requiredPermissions;
    }

    public long getEstimatedEffortMinutes() {
        return estimatedEffortMinutes;
    }

    public void setEstimatedEffortMinutes(long estimatedEffortMinutes) {
        this.estimatedEffortMinutes = estimatedEffortMinutes;
    }

    public SimilarCase[] getSimilarCases() {
        return similarCases;
    }

    public void setSimilarCases(SimilarCase[] similarCases) {
        this.similarCases = similarCases;
    }

    public java.time.Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(java.time.Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}