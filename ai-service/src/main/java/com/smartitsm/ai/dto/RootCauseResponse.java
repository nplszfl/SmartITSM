package com.smartitsm.ai.dto;

public class RootCauseResponse {

    private String ticketId;
    private String rootCause;
    private String confidence;
    private WhyStep[] whyChain;
    private PatternMatch[] patternMatches;
    private String recommendedFix;
    private java.time.Instant analyzedAt;

    public static class WhyStep {
        private int step;
        private String why;
        private String finding;

        public int getStep() {
            return step;
        }

        public void setStep(int step) {
            this.step = step;
        }

        public String getWhy() {
            return why;
        }

        public void setWhy(String why) {
            this.why = why;
        }

        public String getFinding() {
            return finding;
        }

        public void setFinding(String finding) {
            this.finding = finding;
        }
    }

    public static class PatternMatch {
        private String patternId;
        private String patternName;
        private double similarityScore;
        private String knownSolution;

        public String getPatternId() {
            return patternId;
        }

        public void setPatternId(String patternId) {
            this.patternId = patternId;
        }

        public String getPatternName() {
            return patternName;
        }

        public void setPatternName(String patternName) {
            this.patternName = patternName;
        }

        public double getSimilarityScore() {
            return similarityScore;
        }

        public void setSimilarityScore(double similarityScore) {
            this.similarityScore = similarityScore;
        }

        public String getKnownSolution() {
            return knownSolution;
        }

        public void setKnownSolution(String knownSolution) {
            this.knownSolution = knownSolution;
        }
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public WhyStep[] getWhyChain() {
        return whyChain;
    }

    public void setWhyChain(WhyStep[] whyChain) {
        this.whyChain = whyChain;
    }

    public PatternMatch[] getPatternMatches() {
        return patternMatches;
    }

    public void setPatternMatches(PatternMatch[] patternMatches) {
        this.patternMatches = patternMatches;
    }

    public String getRecommendedFix() {
        return recommendedFix;
    }

    public void setRecommendedFix(String recommendedFix) {
        this.recommendedFix = recommendedFix;
    }

    public java.time.Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(java.time.Instant analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}